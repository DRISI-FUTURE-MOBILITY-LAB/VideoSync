import argparse
import os
import re
import shutil
import subprocess
import sys

try:
    # noinspection PyUnresolvedReferences
    import tqdm
except ImportError as err:
    print('\'tqdm\' python module not found. Install \'tqdm\' (e.g. pip3 install tqdm) and use the \'--progress\' '
          'flag to show progress bar for \'--convert\' and \'--fps\' operations. This may cause the \'--convert\''
          'operation to run slower.')


_OG_DAV_RX = re.compile(r"^[a-zA-Z0-9]*_ch[0-9]_[a-zA-Z]*_[0-9]{8}([0-9]{6})_[0-9]{8}([0-9]{6})\.dav$")
_PROBE_TOTAL_FRAMES = re.compile(r"nb_read_frames=([0-9]*)|nb_frames=([0-9]*)")
_PROGRESS_RX = re.compile(r"frame=\s*(\d*)")


def _setup_commandline_args():
    parser = argparse.ArgumentParser(description='TFP - Traffic Footage Processing - Script to help automate the '
                                                 'process of importing traffic intersection footage from a DVR. '
                                                 '')

    parser.add_argument('--progress', '-p', dest='progress_bar', action='store_true',
                        help='Display progress bar for \'--convert\' or \'--fps\' operation. \'--convert\' operation '
                             'will run slower with this flag enabled because video frames must be counted for h264 '
                             'video files to display progress accurately.')

    parser.add_argument('--rename', '-r', dest='rename', action='store_true',
                        help='Rename .dav files from the DVR to a more readable naming scheme')
    parser.add_argument('--convert', '-cv', dest='convert', action='store_true',
                        help='Convert .dav (h264) to .mp4')
    parser.add_argument('--concat', '-cc', dest='concat', action='store_true',
                        help='Convert .mp4 files to intermediate .ts format and concatenate them into one .mp4 file. '
                             'Must specify a directory path, not a file path.')
    parser.add_argument('--fps', '-f', dest='fps', action='store', type=float, nargs=1,
                        help='Convert .mp4 file to specified frame rate')
    parser.add_argument('path', type=str, nargs='?', default='.',
                        help='Specify the path to the file or the directory containing the files you would like to '
                             'perform operations on')

    return parser


def _get_regex_ext(ext: str):
    if not ext:
        ext = '[a-zA-Z0-9]*'

    return r'^([0-9]{1,2})_([0-9]{4})-([0-9]{4})\.' + ext + '$'


def _get_files(video_path):
    # If given path is a file, put it in a list all by itself
    # If given path is a directory, get all files in that directory
    if os.path.isfile(video_path):
        files = [os.path.basename(video_path)]
        video_path = os.path.dirname(video_path)
    else:
        files = os.listdir(video_path)
    os.chdir(video_path)

    return files, video_path


def rename_video_files(video_path: str):
    if not os.path.isabs(video_path):
        video_path = os.path.abspath(video_path)
    print('Path for video file(s): ' + video_path + '\n')

    print('Checking for .dav file(s)...')

    (files, video_path) = _get_files(video_path)
    
    dav_files = list()
    dav_found = False
    name_scheme_matches = True
    for file in files:
        if file.endswith('.dav'):
            if not dav_found:
                print('.dav file(s) found!\n\nChecking .dav file naming scheme...')
                dav_found = True

            if not re.match(_OG_DAV_RX, file):
                if re.match(_get_regex_ext('dav'), file):
                    print('"' + file + '" is already named properly!')
                    continue
                else:
                    print('Naming scheme of .dav file "' + file + '" does not match expected naming scheme')
                    name_scheme_matches = False
            else:
                dav_files.append(file)

    if not dav_found:
        print('Error: .dav file(s) not found! Please enter a path to a .dav file or files')
        return 1

    if name_scheme_matches:
        print('Naming scheme matches for all .dav files!')

    if len(dav_files) == 0:
        print('All .dav files are already named properly or cannot be renamed.')
        return 0

    phase_num = int(input('\nPlease enter the video file phase number: '))

    print('\n' + str(len(dav_files)) + ' file(s) will be renamed: ')
    new_dav_files = list()
    for file in dav_files:
        str_ = str(phase_num)

        matches = re.match(_OG_DAV_RX, file)
        str_ = str_ + '_' + str(int(int(matches.group(1)) / 100))
        str_ = str_ + '-' + str(int(int(matches.group(2)) / 100))
        new_file = str_ + '.dav'

        new_dav_files.append(new_file)

        print('"' + file + '" -> "' + new_file + '"')

    response = input('Continue? (y/n): ')
    print()
    if response.lower() == 'n':
        return 1

    for file, new_file in zip(dav_files, new_dav_files):
        shutil.move(file, new_file)
        print('Renamed "' + file + '" to "' + new_file + '"')


def convert_video_files(video_path: str, show_progress: bool = False):
    if not os.path.isabs(video_path):
        video_path = os.path.abspath(video_path)
    print('Path for video file(s): ' + video_path + '\n')

    print('Checking for .dav files...')

    (files, video_path) = _get_files(video_path)
        
    dav_files = list()
    dav_found = False
    for file in files:
        if file.endswith('.dav'):
            if not dav_found:
                print('.dav files found!\n')
                dav_found = True

            dav_files.append(file)

    if not dav_found:
        print('Error: .dav file(s) not found! Please enter a path to a .dav file or files')
        return 1

    print('Converting h264 to mp4...')
    for file in dav_files:
        total_frames = None
        if show_progress:
            print('Gathering information for \'' + file + '\'...')
            args = ['ffprobe', '-v', 'error', '-count_frames', '-select_streams', 'v:0',
                    '-show_entries', 'stream=nb_read_frames', '-of', 'default=noprint_wrappers=1', file]
            result = subprocess.Popen(args,
                                      shell=False,
                                      stdout=subprocess.PIPE,
                                      stderr=subprocess.PIPE,
                                      universal_newlines=True)

            for line in iter(result.stdout):
                if total_frames is None and _PROBE_TOTAL_FRAMES.search(line):
                    total_frames = _PROBE_TOTAL_FRAMES.search(line).group(1)

            result.wait()

            if result.returncode != 0:
                raise subprocess.CalledProcessError(cmd=args, returncode=result.returncode)

            print('Total Frames: ' + total_frames)

        print('Starting ffmpeg child process to convert "' + file + '"')

        args = ['ffmpeg', '-r', '30', '-i', video_path + '/' + file, '-c:v', 'libx264', file[:-4] + '.mp4']
        if show_progress:
            result = subprocess.Popen(args,
                                      shell=False,
                                      stdout=subprocess.PIPE,
                                      stderr=subprocess.PIPE,
                                      universal_newlines=True)
        else:
            result = subprocess.run(args, check=True)

        if show_progress:
            p_bar = tqdm.tqdm(total=int(total_frames), leave=True, unit=' frames', dynamic_ncols=True, position=0)

            for line in iter(result.stderr):
                if _PROGRESS_RX.search(line):
                    p_bar.update(int(_PROGRESS_RX.search(line).group(1)) - p_bar.n)
            p_bar.close()

            result.wait()

        if result.returncode == 0:
            print('ffmpeg conversion completed successfully!\n')


def concat_video_files(video_path: str):
    if not os.path.isabs(video_path):
        video_path = os.path.abspath(video_path)
    print('Path for video files: ' + video_path + '\n')

    print('Checking for .mp4 files...')

    # If given path is a file, convert path to directory path
    if os.path.isfile(video_path):
        response = input('You must specify a directory path containing multiple files, not a file path, for the '
                         '\'--concat\' operation. Would you like to use \'' + os.path.dirname(video_path) +
                         '\' instead? (Y/n): ')

        if response.lower() != 'y':
            print('Error: Please try again using a directory path instead of a file path.')
            return 1

        video_path = os.path.dirname(video_path)

    files = os.listdir(video_path)
    os.chdir(video_path)

    mp4_files = list()
    mp4_found = False
    for file in files:
        if file.endswith('.mp4'):
            if not mp4_found:
                print('.mp4 files found!\n\nChecking .mp4 file naming scheme...')
                mp4_found = True

            if not re.match(_get_regex_ext('mp4'), file):
                print('Error: Some .mp4 files don\'t match expected naming scheme')
                return 1

            mp4_files.append(file)

    if not mp4_found:
        print('Error: .mp4 file(s) not found! Please enter a path to a directory containing .mp4 files')
        return 1

    if len(mp4_files) < 2:
        print('Error: Cannot concatenate just one mp4 file!')
        return 1

    print('Naming scheme matches for all .mp4 files!\n\nCreating mpeg intermediates...')
    intermediate_files = list()
    for file in mp4_files:
        print('Starting ffmpeg child process to create mpeg intermediate for "' + file + '"')

        new_file = file[:-4] + '.ts'
        intermediate_files.append(new_file)
        args = ['ffmpeg', '-i', video_path + '/' + file, '-c', 'copy', '-bsf:v', 'h264_mp4toannexb', '-f', 'mpegts',
                new_file]
        result = subprocess.run(args, check=True)

        if result.returncode == 0:
            print('mpeg intermediate creation completed successfully!\n')

    intermediate_files = sorted(intermediate_files,
                                key=lambda file_name: (re.match(_get_regex_ext(ext=''), file_name)).group(2))

    print('Concatenating intermediates...')
    print('Starting ffmpeg child process to concatenate ' + str(len(intermediate_files)) + ' files')
    intermediate_string = 'concat:' + '|'.join(intermediate_files)

    phase_num = re.match(_get_regex_ext(ext=''), intermediate_files[0]).group(1)
    first_time = re.match(_get_regex_ext(ext=''), intermediate_files[0]).group(2)
    last_time = re.match(_get_regex_ext(ext=''), intermediate_files[len(intermediate_files) - 1]).group(3)
    final_file = phase_num + '_' + first_time + '-' + last_time + '.mp4'

    args = ['ffmpeg', '-i', intermediate_string, '-c', 'copy', '-bsf:a', 'aac_adtstoasc', final_file]
    result = subprocess.run(args,
                            shell=False,
                            stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE,
                            universal_newlines=True)

    if result.returncode == 0:
        print('Concatenation completed successfully!\n')

    print('Cleaning up...')
    for file in mp4_files:
        os.remove(file)

    for file in intermediate_files:
        os.remove(file)


def set_fps(video_path: str, fps: str = '29.97', show_progress: bool = False):
    if not os.path.isabs(video_path):
        video_path = os.path.abspath(video_path)
    print('Path for video file(s): ' + video_path + '\n')

    (files, video_path) = _get_files(video_path)

    print('Checking for .mp4 files...')
    mp4_files = list()
    for file in files:
        if file.endswith('.mp4'):
            mp4_files.append(file)

    if mp4_files:
        print('.mp4 file(s) found!')
    else:
        print('Error: No .mp4 file(s) found at given path!')
        return 1

    for file in mp4_files:
        # Get total frames
        print('\nGathering information for \'' + file + '\'...')
        args = ['ffprobe', '-v', 'error', '-select_streams', 'v:0', '-show_entries',
                'stream=nb_frames', '-of', 'default=noprint_wrappers=1', file]
        result = subprocess.Popen(args,
                                  shell=False,
                                  stdout=subprocess.PIPE,
                                  stderr=subprocess.PIPE,
                                  universal_newlines=True)

        total_frames = None
        for line in iter(result.stdout):
            if total_frames is None and _PROBE_TOTAL_FRAMES.search(line):
                total_frames = _PROBE_TOTAL_FRAMES.search(line).group(2)

        result.wait()

        if result.returncode != 0:
            raise subprocess.CalledProcessError(cmd=args, returncode=result.returncode)

        print('Total Frames: ' + total_frames)

        print('Converting frame rate to ' + fps)

        args = ['ffmpeg', '-r', fps, '-i', file, '-c:v', 'libx264',
                '-pix_fmt', 'yuv420p', '-crf', '23', file[:-4] + '_' + fps + '.mp4']
        if show_progress:
            p_bar = tqdm.tqdm(total=int(total_frames), leave=True, unit=' frames', dynamic_ncols=True, position=0)

            result = subprocess.Popen(args,
                                      shell=False,
                                      stdout=subprocess.PIPE,
                                      stderr=subprocess.PIPE,
                                      universal_newlines=True)

            for line in iter(result.stderr):
                if _PROGRESS_RX.search(line):
                    p_bar.update(int(_PROGRESS_RX.search(line).group(1)) - p_bar.n)
            p_bar.close()

            result.wait()
        else:
            result = subprocess.run(args, check=True)

        if result.returncode == 0:
            print('Frame rate conversion completed successfully!\n')
            os.remove(file)


def main():
    # Check if ffmpeg is installed
    try:
        subprocess.run(['ffmpeg', '-version'], capture_output=True, check=True)
    except FileNotFoundError as error:
        print(error)
        print('This script requires ffmpeg to be installed')
        return

    parser = _setup_commandline_args()
    args = parser.parse_args()

    progress_bar = args.progress_bar
    if args.progress_bar and 'tqdm' not in sys.modules:
        progress_bar = False

    any_ = args.rename or args.convert or args.concat or args.fps
    if any_:
        og_dir = os.getcwd()
        if args.rename:
            if rename_video_files(video_path=args.path) == 1:
                return
            os.chdir(og_dir)
        if args.convert:
            if convert_video_files(video_path=args.path, show_progress=progress_bar) == 1:
                return
            os.chdir(og_dir)
        if args.concat:
            if concat_video_files(video_path=args.path) == 1:
                return
            os.chdir(og_dir)
        if args.fps:
            fps = args.fps[0]
            if fps.is_integer():
                fps = int(fps)
            if set_fps(video_path=args.path, fps=str(fps), show_progress=progress_bar) == 1:
                return
            os.chdir(og_dir)
    else:
        parser.print_usage()
        print('No operation specified. Please add \'--rename\', \'--convert\', \'--concat\', or \'--fps\'.')


if __name__ == '__main__':
    sys.exit(main())
