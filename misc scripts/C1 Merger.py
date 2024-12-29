import os
import sys
import re


def main():
    output_mapping_path = 'output mapping.txt'
    merged_c1_path = 'output.c1'
    
    # Pass in and load two C1 text files
    if len(sys.argv) < 3:
        print('You need to pass in two C1 files as command line arguments.')
        return

    file_one_dir = os.path.abspath(sys.argv[1])
    file_two_dir = os.path.abspath(sys.argv[2])

    # Make sure they are C1 files (extension ends with .c1)
    if not str.endswith(file_one_dir, '.c1') or not str.endswith(file_two_dir, '.c1'):
        print('Files must be .c1 files.')
        return

    file_one = open(file_one_dir, 'r')
    file_two = open(file_two_dir, 'r')

    # Load each file into a list
    c1_one = [x for x in file_one.read().split('\n') if x and not x.startswith('Pi')]
    c1_two = [x for x in file_two.read().split('\n') if x and not x.startswith('Pi')]

    file_one.close()
    file_two.close()

    if len(c1_one) == 0 or len(c1_two) == 0:
        print('One of the C1 files is empty.')
        return

    # Find first line matching C1 regex (both files) as validation
    regex = r'([0-9]) ([0-9]{1,2}) ([01]) ([0-9]*)'
    match_one = re.search(regex, c1_one[0])
    match_two = re.search(regex, c1_two[0])

    if not match_one or not match_two:
        print('At least one file did not match C1 data format.')
        return

    # Sync files based on known offsets
    offset_one = None
    while True:
        answer = input('Enter video offset for ' + os.path.basename(file_one_dir) + ' in milliseconds: ')
        try:
            offset_one = int(answer)
        except ValueError:
            print('Error: Invalid input. Please enter an integer.')
            continue

        break

    offset_two = None
    while True:
        answer = input('\nEnter video offset for ' + os.path.basename(file_two_dir) + ' in milliseconds: ')
        try:
            offset_two = int(answer)
        except ValueError:
            print('Error: Invalid input. Please enter an integer.')
            continue

        break

    offset = abs(offset_one - offset_two)
    if offset_one > offset_two:
        c1_one_ts = int(match_one.group(4))
        c1_two_ts = int(match_two.group(4))

        c1_one = [
            re.sub(regex, r'\1 \2 \3 ' + str(int(re.search(regex, line).group(4)) - c1_one_ts).zfill(10), line)
            for line in c1_one
        ]

        c1_two = [
            re.sub(regex, r'\1 \2 \3 ' + str(int(re.search(regex, line).group(4)) - c1_two_ts + offset).zfill(10), line)
            for line in c1_two
        ]
    elif offset_one < offset_two:
        c1_one_ts = int(match_one.group(4))
        c1_two_ts = int(match_two.group(4))
        c1_one = [
            re.sub(regex, r'\1 \2 \3 ' + str(int(re.search(regex, line).group(4)) - c1_one_ts + offset).zfill(10), line)
            for line in c1_one
        ]

        c1_two = [
            re.sub(regex, r'\1 \2 \3 ' + str(int(re.search(regex, line).group(4)) - c1_two_ts).zfill(10), line)
            for line in c1_two
        ]

    # Create set of chip-pin pairs in first file
    used_channels = set()
    for line in c1_one:
        line_match = re.search(regex, line)
        pair = (line_match.group(1), line_match.group(2))
        used_channels.add(pair)

    # Create set of available pairs (those in first file are removed)
    available_channels = [
        (line[0:1], line[2:4]) for line in open('channels.txt', 'r').read().split('\n') if line[0:1] and line[2:4]
    ]
    available_channels = list(set(available_channels) - used_channels)
    available_channels.sort()

    # Check each unique chip-pin pair in second file and check if it exists in first file
    # If yes, map it to the next available pair
    mapping = dict()
    new_c1_two = list()
    for line in c1_two:
        line_match = re.search(regex, line)
        pair = (line_match.group(1), line_match.group(2))
        if pair in mapping:
            new_pair = mapping[pair]
            new_line = re.sub(regex, new_pair[0] + ' ' + new_pair[1] + r' \3 \4', line)
            new_c1_two.append(new_line)
        elif pair in used_channels:
            new_pair = available_channels.pop(0)
            mapping[pair] = new_pair
            new_line = re.sub(regex, new_pair[0] + ' ' + new_pair[1] + r' \3 \4', line)
            new_c1_two.append(new_line)
        else:
            new_c1_two.append(line)
    c1_two = new_c1_two

    # Once complete, combine files into one collection and sort by timestamp
    output_c1 = c1_one + c1_two
    output_c1.sort(key=lambda x: int(re.search(regex, x).group(4)))

    # Save new C1 file and generate .txt with new mapping
    print('Saving merged c1 file to: ' + merged_c1_path)
    f = open(merged_c1_path, 'w')
    f.write('\n'.join(output_c1))
    f.close()

    map_output = list()
    for key, value in mapping.items():
        map_output.append(key[0] + ' ' + key[1] + ' --> ' + value[0] + ' ' + value[1])


    print('Saving output mapping to: ' + output_mapping_path)
    f = open(output_mapping_path, 'w+')
    f.write('\n'.join(map_output))
    f.close()


if __name__ == '__main__':
    main()
