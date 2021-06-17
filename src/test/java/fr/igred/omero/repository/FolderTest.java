/*
 *  Copyright (C) 2020 GReD
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.

 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package fr.igred.omero.repository;


import fr.igred.omero.UserTest;
import fr.igred.omero.roi.ROIWrapper;
import fr.igred.omero.roi.RectangleWrapper;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;


public class FolderTest extends UserTest {


    @Test
    public void testFolder1() throws Exception {
        boolean exception = false;

        FolderWrapper folder = new FolderWrapper(client, "Test1");
        try {
            RectangleWrapper rectangle = new RectangleWrapper(client, 0, 0, 10, 10);
            rectangle.setZ(0);
            rectangle.setT(0);
            rectangle.setC(0);

            ROIWrapper roi = new ROIWrapper(client);
            roi.addShape(rectangle);
            roi.saveROI();

            folder.addROI(roi);
        } catch (Exception e) {
            exception = true;
        }
        assertTrue(exception);
    }


    @Test
    public void testFolder2() throws Exception {
        ImageWrapper image = client.getImage(3L);

        FolderWrapper folder = new FolderWrapper(client, "Test");
        folder.setImage(image);

        for (int i = 0; i < 8; i++) {
            ROIWrapper roi = new ROIWrapper(client);

            RectangleWrapper rectangle = new RectangleWrapper(client);
            rectangle.setCoordinates(i * 2, i * 2, 10, 10);
            rectangle.setZ(0);
            rectangle.setT(0);
            rectangle.setC(0);

            roi.addShape(rectangle);
            roi.saveROI();

            folder.addROI(roi);
        }

        folder = image.getFolder(folder.getId());
        List<ROIWrapper> rois = folder.getROIs();
        assertEquals(8, rois.size());
        assertEquals("Test", folder.getName());
        assertEquals(8, image.getROIs().size());

        for (ROIWrapper roi : rois) {
            client.deleteROI(roi);
        }

        rois = folder.getROIs();
        assertEquals(0, rois.size());
        assertEquals(0, image.getROIs().size());

        client.deleteFolder(folder);

        try {
            image.getFolder(folder.getId());
            fail();
        } catch (Exception e) {
            assertTrue(true);
        }
    }


    @Test
    public void testFolder3() throws Exception {
        FolderWrapper folder = new FolderWrapper(client, "Test");
        folder.setImage(3L);

        for (int i = 0; i < 8; i++) {
            RectangleWrapper rectangle = new RectangleWrapper(client);
            rectangle.setCoordinates(i * 2, i * 2, 10, 10);
            rectangle.setZ(0);
            rectangle.setT(0);
            rectangle.setC(0);

            ROIWrapper roi = new ROIWrapper(client);
            roi.addShape(rectangle);
            roi.saveROI();

            folder.addROI(roi);
        }

        List<ROIWrapper> rois = folder.getROIs();
        assertEquals(8, rois.size());

        folder.unlinkROI(rois.get(0));
        client.deleteROI(rois.get(0));
        rois = folder.getROIs();
        assertEquals(7, rois.size());

        client.deleteFolder(folder);

        for (ROIWrapper roi : rois) {
            client.deleteROI(roi);
        }
    }


    @Test
    public void testFolder4() throws Exception {
        ImageWrapper image = client.getImage(3L);

        FolderWrapper folder = new FolderWrapper(client, "Test1");
        folder.setImage(image);

        for (int i = 0; i < 8; i++) {
            ROIWrapper roi = new ROIWrapper(client);

            RectangleWrapper rectangle = new RectangleWrapper(client);
            rectangle.setCoordinates(i * 2, i * 2, 10, 10);
            rectangle.setZ(0);
            rectangle.setT(0);
            rectangle.setC(0);

            roi.addShape(rectangle);
            roi.saveROI();

            folder.addROI(roi);
        }

        folder = new FolderWrapper(client, "Test2");
        folder.setImage(image);

        for (int i = 0; i < 8; i++) {
            ROIWrapper roi = new ROIWrapper(client);

            RectangleWrapper rectangle = new RectangleWrapper(client);
            rectangle.setCoordinates(i * 2, i * 2, 5, 5);
            rectangle.setZ(i);
            rectangle.setT(0);
            rectangle.setC(0);

            roi.addShape(rectangle);
            roi.saveROI();

            folder.addROI(roi);
        }

        List<FolderWrapper> folders = image.getFolders();
        assertEquals(2, folders.size());
        assertEquals(16, image.getROIs().size());

        for (FolderWrapper RoiFolder : folders) {
            client.deleteFolder(RoiFolder);
        }

        folders = image.getFolders();
        assertEquals(0, folders.size());
        assertEquals(16, image.getROIs().size());

        List<ROIWrapper> rois = image.getROIs();
        for (ROIWrapper roi : rois) {
            client.deleteROI(roi);
        }

        assertEquals(0, image.getROIs().size());
    }

}
