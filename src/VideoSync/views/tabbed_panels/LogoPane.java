/*
 * ****************************************************************
 * File: 			LogoPane.java
 * Date Created:  	April 25, 2017
 * Programmer:		Elliot Hawkins
 *
 * Purpose:			To display the Caltrans logo for legal purposes.
 *
 * ****************************************************************
 */
package VideoSync.views.tabbed_panels;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

public class LogoPane extends JPanel
{
    private BufferedImage caltransLogo;

    public LogoPane()
    {
        try
        {
            //Attempt to load image from resource
            URL logoResourceURL = getClass().getClassLoader().getResource("logo.png");
            if(logoResourceURL != null)
            {
                caltransLogo = ImageIO.read(logoResourceURL);
            }
            else
            {
                //Image is not in resources. Attempt to load as file.
                caltransLogo = ImageIO.read(new File("logo.png"));
            }
        }
        catch(IOException ex)
        {
            System.out.println("Failed to load Caltrans logo!");
            ex.printStackTrace();
        }
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        int renderX = 0;
        int renderY = 0;
        int renderWidth;
        int renderHeight;

        //Don't attempt to render if logo didn't load properly.
        if(caltransLogo != null)
        {

            //Calculate needed scaling to fit panel
            if(this.getWidth() > this.getHeight())
            {
                renderWidth = getWidth();
                renderHeight = (int) ((float) getHeight() * ((float) caltransLogo.getHeight() / (float) caltransLogo.getWidth()));
                renderY = (getHeight() / 2) - (renderHeight / 2);
            }
            else
            {
                renderWidth = (int) ((float) getWidth() * ((float) caltransLogo.getWidth() / (float) caltransLogo.getHeight()));
                renderHeight = getHeight();
                renderX = (getWidth() / 2) - (renderWidth / 2);
            }

            //Render the logo
            g.drawImage(caltransLogo, renderX, renderY, renderWidth, renderHeight, this);
        }
    }
}
