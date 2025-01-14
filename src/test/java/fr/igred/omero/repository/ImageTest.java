/*
 *  Copyright (C) 2020-2022 GReD
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
import fr.igred.omero.annotations.FileAnnotationWrapper;
import fr.igred.omero.annotations.MapAnnotationWrapper;
import fr.igred.omero.annotations.TableWrapper;
import fr.igred.omero.annotations.TagAnnotationWrapper;
import fr.igred.omero.roi.EllipseWrapper;
import fr.igred.omero.roi.ROIWrapper;
import fr.igred.omero.roi.RectangleWrapper;
import ij.ImagePlus;
import ij.plugin.Duplicator;
import ij.plugin.ImageCalculator;
import ij.process.ImageStatistics;
import loci.plugins.BF;
import omero.gateway.model.MapAnnotationData;
import omero.model.NamedValue;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class ImageTest extends UserTest {


    @Test
    public void testImportImage() throws Exception {
        String filename1 = "8bit-unsigned&pixelType=uint8&sizeZ=5&sizeC=5&sizeT=7&sizeX=512&sizeY=512.fake";
        String filename2 = "8bit-unsigned&pixelType=uint8&sizeZ=4&sizeC=5&sizeT=6&sizeX=512&sizeY=512.fake";

        File f1 = new File("." + File.separator + filename1);
        if (!f1.createNewFile())
            System.err.println("\"" + f1.getCanonicalPath() + "\" could not be created.");

        File f2 = new File("." + File.separator + filename2);
        if (!f2.createNewFile())
            System.err.println("\"" + f2.getCanonicalPath() + "\" could not be created.");

        DatasetWrapper dataset = client.getDataset(DATASET2.id);

        boolean imported = dataset.importImages(client, f1.getAbsolutePath(), f2.getAbsolutePath());

        if (!f1.delete())
            System.err.println("\"" + f1.getCanonicalPath() + "\" could not be deleted.");

        if (!f2.delete())
            System.err.println("\"" + f2.getCanonicalPath() + "\" could not be deleted.");

        List<ImageWrapper> images = dataset.getImages(client);

        for (ImageWrapper image : images) {
            client.delete(image);
        }

        List<ImageWrapper> endImages = dataset.getImages(client);

        assertEquals(2, images.size());
        assertTrue(endImages.isEmpty());
        assertTrue(imported);
    }


    @Test
    public void testPairKeyValue() throws Exception {
        String filename = "8bit-unsigned&pixelType=uint8&sizeZ=3&sizeC=5&sizeT=7&sizeX=512&sizeY=512.fake";

        File f = new File("." + File.separator + filename);
        if (!f.createNewFile())
            System.err.println("\"" + f.getCanonicalPath() + "\" could not be created.");

        DatasetWrapper dataset = client.getDataset(DATASET2.id);

        List<Long> newIDs = dataset.importImage(client, f.getAbsolutePath());
        assertEquals(1, newIDs.size());

        if (!f.delete())
            System.err.println("\"" + f.getCanonicalPath() + "\" could not be deleted.");

        List<ImageWrapper> images = dataset.getImages(client);

        ImageWrapper image = images.get(0);

        List<NamedValue> result1 = new ArrayList<>(2);
        result1.add(new NamedValue("Test result1", "Value Test"));
        result1.add(new NamedValue("Test2 result1", "Value Test2"));

        Collection<NamedValue> result2 = new ArrayList<>(2);
        result2.add(new NamedValue("Test result2", "Value Test"));
        result2.add(new NamedValue("Test2 result2", "Value Test2"));

        MapAnnotationWrapper mapAnnotation1 = new MapAnnotationWrapper(result1);

        MapAnnotationData mapData2 = new MapAnnotationData();
        mapData2.setContent(result2);
        MapAnnotationWrapper mapAnnotation2 = new MapAnnotationWrapper(mapData2);

        assertEquals(result1, mapAnnotation1.getContent());

        image.addMapAnnotation(client, mapAnnotation1);
        image.addMapAnnotation(client, mapAnnotation2);

        Map<String, String> result = image.getKeyValuePairs(client);

        assertEquals(4, result.size());
        assertEquals("Value Test", image.getValue(client, "Test result1"));

        client.delete(image);
    }


    @Test
    public void testPairKeyValue2() throws Exception {
        String filename = "8bit-unsigned&pixelType=uint8&sizeZ=3&sizeC=5&sizeT=7&sizeX=512&sizeY=512.fake";

        File f = new File("." + File.separator + filename);
        if (!f.createNewFile())
            System.err.println("\"" + f.getCanonicalPath() + "\" could not be created.");

        DatasetWrapper dataset = client.getDataset(DATASET2.id);

        dataset.importImages(client, f.getAbsolutePath());

        if (!f.delete())
            System.err.println("\"" + f.getCanonicalPath() + "\" could not be deleted.");

        List<ImageWrapper> images = dataset.getImages(client);

        ImageWrapper image = images.get(0);

        List<NamedValue> result = new ArrayList<>(2);
        result.add(new NamedValue("Test result1", "Value Test"));
        result.add(new NamedValue("Test2 result1", "Value Test2"));

        MapAnnotationWrapper mapAnnotation = new MapAnnotationWrapper();
        mapAnnotation.setContent(result);

        image.addMapAnnotation(client, mapAnnotation);

        Map<String, String> results = image.getKeyValuePairs(client);

        assertEquals(2, results.size());
        assertEquals("Value Test", image.getValue(client, "Test result1"));

        client.delete(image);
    }


    @Test
    public void testPairKeyValue3() throws Exception {
        boolean exception = false;

        String filename = "8bit-unsigned&pixelType=uint8&sizeZ=3&sizeC=5&sizeT=7&sizeX=512&sizeY=512.fake";

        File f = new File("." + File.separator + filename);
        if (!f.createNewFile())
            System.err.println("\"" + f.getCanonicalPath() + "\" could not be created.");

        DatasetWrapper dataset = client.getDataset(DATASET2.id);

        dataset.importImages(client, f.getAbsolutePath());

        if (!f.delete())
            System.err.println("\"" + f.getCanonicalPath() + "\" could not be deleted.");

        List<ImageWrapper> images = dataset.getImages(client);

        ImageWrapper image = images.get(0);

        image.addPairKeyValue(client, "Test result1", "Value Test");
        image.addPairKeyValue(client, "Test result2", "Value Test2");

        Map<String, String> results = image.getKeyValuePairs(client);

        assertEquals(2, results.size());
        try {
            image.getValue(client, "Nonexistent value");
        } catch (Exception e) {
            exception = true;
        }
        client.delete(image);
        assertTrue(exception);
    }


    @Test
    public void testGetImageInfo() throws Exception {
        ImageWrapper image = client.getImage(IMAGE1.id);
        assertEquals(IMAGE1.name, image.getName());
        assertNull(image.getDescription());
        assertEquals(1L, image.getId());
    }


    @Test
    public void testGetImageTag() throws Exception {
        ImageWrapper image = client.getImage(IMAGE1.id);

        List<TagAnnotationWrapper> tags = image.getTags(client);
        assertEquals(2, tags.size());
    }


    @Test
    public void testGetImageSize() throws Exception {
        final int sizeXY = 512;
        final int sizeC  = 5;
        final int sizeZ  = 3;
        final int sizeT  = 7;

        ImageWrapper  image  = client.getImage(IMAGE1.id);
        PixelsWrapper pixels = image.getPixels();

        assertEquals(sizeXY, pixels.getSizeX());
        assertEquals(sizeXY, pixels.getSizeY());
        assertEquals(sizeC, pixels.getSizeC());
        assertEquals(sizeZ, pixels.getSizeZ());
        assertEquals(sizeT, pixels.getSizeT());
    }


    @Test
    public void testGetRawData() throws Exception {
        ImageWrapper     image  = client.getImage(IMAGE1.id);
        PixelsWrapper    pixels = image.getPixels();
        double[][][][][] value  = pixels.getAllPixels(client);

        assertEquals(pixels.getSizeX(), value[0][0][0][0].length);
        assertEquals(pixels.getSizeY(), value[0][0][0].length);
        assertEquals(pixels.getSizeC(), value[0][0].length);
        assertEquals(pixels.getSizeZ(), value[0].length);
        assertEquals(pixels.getSizeT(), value.length);
    }


    @Test
    public void testGetRawData2() throws Exception {
        ImageWrapper  image  = client.getImage(IMAGE1.id);
        PixelsWrapper pixels = image.getPixels();
        byte[][][][]  value  = pixels.getRawPixels(client, 1);

        int sizeX = pixels.getSizeX();
        int sizeY = pixels.getSizeY();
        int sizeZ = pixels.getSizeZ();
        int sizeC = pixels.getSizeC();
        int sizeT = pixels.getSizeT();

        assertEquals(sizeX * sizeY, value[0][0][0].length);
        assertEquals(sizeC, value[0][0].length);
        assertEquals(sizeZ, value[0].length);
        assertEquals(sizeT, value.length);
    }


    @Test
    public void testGetRawDataBound() throws Exception {
        ImageWrapper  image  = client.getImage(IMAGE1.id);
        PixelsWrapper pixels = image.getPixels();

        int[] xBound = {0, 2};
        int[] yBound = {0, 2};
        int[] cBound = {0, 2};
        int[] zBound = {0, 2};
        int[] tBound = {0, 2};

        double[][][][][] value = pixels.getAllPixels(client, xBound, yBound, cBound, zBound, tBound);

        assertEquals(3, value[0][0][0][0].length);
        assertEquals(3, value[0][0][0].length);
        assertEquals(3, value[0][0].length);
        assertEquals(3, value[0].length);
        assertEquals(3, value.length);
    }


    @Test
    public void testGetRawDataBoundError() throws Exception {
        ImageWrapper  image  = client.getImage(IMAGE1.id);
        PixelsWrapper pixels = image.getPixels();

        final int[] xBound = {511, 513};
        final int[] yBound = {0, 2};
        final int[] cBound = {0, 2};
        final int[] zBound = {0, 2};
        final int[] tBound = {0, 2};

        double[][][][][] value = pixels.getAllPixels(client, xBound, yBound, cBound, zBound, tBound);
        assertNotEquals(xBound[1] - xBound[0] + 1, value[0][0][0][0].length);
    }


    @Test
    public void testGetRawDataBoundErrorNegative() throws Exception {
        boolean success = true;

        ImageWrapper  image  = client.getImage(IMAGE1.id);
        PixelsWrapper pixels = image.getPixels();

        int[] xBound = {-1, 1};
        int[] yBound = {0, 2};
        int[] cBound = {0, 2};
        int[] zBound = {0, 2};
        int[] tBound = {0, 2};
        try {
            double[][][][][] value = pixels.getAllPixels(client, xBound, yBound, cBound, zBound, tBound);
            success = false;
            assertNotEquals(3, value[0][0][0][0].length);
        } catch (Exception e) {
            assertTrue(success);
        }
    }


    @Test
    public void testToImagePlusBound() throws Exception {
        final int    lowXY   = 500;
        final int    highXY  = 507;
        final double pixSize = 0.5;

        int[] xBound = {0, 2};
        int[] yBound = {0, 2};
        int[] cBound = {0, 2};
        int[] zBound = {0, 2};
        int[] tBound = {0, 2};

        Random random = new SecureRandom();
        xBound[0] = random.nextInt(lowXY);
        yBound[0] = random.nextInt(lowXY);
        cBound[0] = random.nextInt(3);
        tBound[0] = random.nextInt(5);
        xBound[1] = random.nextInt(highXY - xBound[0]) + xBound[0] + 5;
        yBound[1] = random.nextInt(highXY - yBound[0]) + yBound[0] + 5;
        cBound[1] = random.nextInt(3 - cBound[0]) + cBound[0] + 2;
        tBound[1] = random.nextInt(5 - tBound[0]) + tBound[0] + 2;

        String fake     = "8bit-unsigned&pixelType=uint8&sizeZ=3&sizeC=5&sizeT=7&sizeX=512&sizeY=512.fake";
        File   fakeFile = new File(fake);

        if (!fakeFile.createNewFile())
            System.err.println("\"" + fakeFile.getCanonicalPath() + "\" could not be created.");

        ImagePlus reference = BF.openImagePlus(fake)[0];

        if (!fakeFile.delete())
            System.err.println("\"" + fakeFile.getCanonicalPath() + "\" could not be deleted.");

        Duplicator duplicator = new Duplicator();
        reference.setRoi(xBound[0], yBound[0], xBound[1] - xBound[0] + 1, yBound[1] - yBound[0] + 1);
        ImagePlus crop = duplicator.run(reference,
                                        cBound[0] + 1, cBound[1] + 1,
                                        zBound[0] + 1, zBound[1] + 1,
                                        tBound[0] + 1, tBound[1] + 1);

        ImageWrapper image = client.getImage(IMAGE1.id);

        ImagePlus imp = image.toImagePlus(client, xBound, yBound, cBound, zBound, tBound);

        ImageCalculator calculator = new ImageCalculator();
        ImagePlus       difference = calculator.run("difference create stack", crop, imp);
        ImageStatistics stats      = difference.getStatistics();

        assertEquals(pixSize, imp.getCalibration().pixelHeight, Double.MIN_VALUE);
        assertEquals(pixSize, imp.getCalibration().pixelWidth, Double.MIN_VALUE);
        assertEquals(1.0, imp.getCalibration().pixelDepth, Double.MIN_VALUE);
        assertEquals("MICROMETER", imp.getCalibration().getUnit());
        assertEquals(0, (int) stats.max);
    }


    @Test
    public void testToImagePlus() throws Exception {
        String fake = "8bit-unsigned&pixelType=uint8&sizeZ=2&sizeC=5&sizeT=7&sizeX=512&sizeY=512.fake";

        File fakeFile = new File(fake);

        if (!fakeFile.createNewFile())
            System.err.println("\"" + fakeFile.getCanonicalPath() + "\" could not be created.");

        ImagePlus reference = BF.openImagePlus(fake)[0];

        if (!fakeFile.delete())
            System.err.println("\"" + fakeFile.getCanonicalPath() + "\" could not be deleted.");

        ImageWrapper image = client.getImage(IMAGE2.id);

        ImagePlus imp = image.toImagePlus(client);

        ImageCalculator calculator = new ImageCalculator();
        ImagePlus       difference = calculator.run("difference create stack", reference, imp);
        ImageStatistics stats      = difference.getStatistics();

        assertEquals(0, (int) stats.max);
    }


    @Test
    public void testGetImageChannel() throws Exception {
        ImageWrapper image = client.getImage(IMAGE1.id);
        assertEquals("0", image.getChannelName(client, 0));
    }


    @Test
    public void testGetImageChannelError() throws Exception {
        boolean success = true;

        ImageWrapper image = client.getImage(IMAGE1.id);
        try {
            image.getChannelName(client, 6);
            success = false;
            fail();
        } catch (Exception e) {
            assertTrue(success);
        }
    }


    @Test
    public void testAddTagToImage() throws Exception {
        ImageWrapper image = client.getImage(IMAGE2.id);

        TagAnnotationWrapper tag = new TagAnnotationWrapper(client, "image tag", "tag attached to an image");

        image.addTag(client, tag);

        List<TagAnnotationWrapper> tags = image.getTags(client);
        client.delete(tag);
        List<TagAnnotationWrapper> endTags = image.getTags(client);

        assertEquals(1, tags.size());
        assertEquals(0, endTags.size());
    }


    @Test
    public void testAddTagToImage2() throws Exception {
        ImageWrapper image = client.getImage(IMAGE2.id);

        image.addTag(client, "image tag", "tag attached to an image");

        List<TagAnnotationWrapper> tags = client.getTags("image tag");
        client.delete(tags.get(0));
        List<TagAnnotationWrapper> endTags = client.getTags("image tag");

        assertEquals(1, tags.size());
        assertEquals(0, endTags.size());
    }


    @Test
    public void testAddTagIdToImage() throws Exception {
        ImageWrapper image = client.getImage(IMAGE2.id);

        TagAnnotationWrapper tag = new TagAnnotationWrapper(client, "image tag", "tag attached to an image");

        image.addTag(client, tag.getId());

        List<TagAnnotationWrapper> tags = image.getTags(client);
        client.delete(tag);
        List<TagAnnotationWrapper> endTags = image.getTags(client);

        assertEquals(1, tags.size());
        assertEquals(0, endTags.size());
    }


    @Test
    public void testAddTagsToImage() throws Exception {
        ImageWrapper image = client.getImage(IMAGE2.id);

        TagAnnotationWrapper tag1 = new TagAnnotationWrapper(client, "Image tag 1", "tag attached to an image");
        TagAnnotationWrapper tag2 = new TagAnnotationWrapper(client, "Image tag 2", "tag attached to an image");
        TagAnnotationWrapper tag3 = new TagAnnotationWrapper(client, "Image tag 3", "tag attached to an image");
        TagAnnotationWrapper tag4 = new TagAnnotationWrapper(client, "Image tag 4", "tag attached to an image");

        image.addTags(client, tag1.getId(), tag2.getId(), tag3.getId(), tag4.getId());
        List<TagAnnotationWrapper> tags = image.getTags(client);
        client.delete(tag1);
        client.delete(tag2);
        client.delete(tag3);
        client.delete(tag4);
        List<TagAnnotationWrapper> endTags = image.getTags(client);

        assertEquals(4, tags.size());
        assertEquals(0, endTags.size());
    }


    @Test
    public void testAddTagsToImage2() throws Exception {
        ImageWrapper image = client.getImage(IMAGE2.id);

        TagAnnotationWrapper tag1 = new TagAnnotationWrapper(client, "Image tag 1", "tag attached to an image");
        TagAnnotationWrapper tag2 = new TagAnnotationWrapper(client, "Image tag 2", "tag attached to an image");
        TagAnnotationWrapper tag3 = new TagAnnotationWrapper(client, "Image tag 3", "tag attached to an image");
        TagAnnotationWrapper tag4 = new TagAnnotationWrapper(client, "Image tag 4", "tag attached to an image");

        image.addTags(client, tag1, tag2, tag3, tag4);
        List<TagAnnotationWrapper> tags = image.getTags(client);
        client.delete(tag1);
        client.delete(tag2);
        client.delete(tag3);
        client.delete(tag4);
        List<TagAnnotationWrapper> endTags = image.getTags(client);

        assertEquals(4, tags.size());
        assertEquals(0, endTags.size());
    }


    @Test
    public void testAddAndRemoveTagFromImage() throws Exception {
        ImageWrapper image = client.getImage(IMAGE2.id);

        TagAnnotationWrapper tag = new TagAnnotationWrapper(client, "Dataset tag", "tag attached to an image");

        image.addTag(client, tag);

        List<TagAnnotationWrapper> tags = image.getTags(client);
        image.unlink(client, tag);
        List<TagAnnotationWrapper> removed = image.getTags(client);
        client.delete(tag);

        assertEquals(1, tags.size());
        assertEquals(0, removed.size());
    }


    @Test
    public void testImageOrder() throws Exception {
        List<ImageWrapper> images = client.getImages();
        for (int i = 1; i < images.size(); i++) {
            assertTrue(images.get(i - 1).getId() <= images.get(i).getId());
        }
    }


    @Test
    public void testAddFileImage() throws Exception {
        final String tmpdir = System.getProperty("java.io.tmpdir") + File.separator;

        ImageWrapper image = client.getImage(IMAGE1.id);

        File file = new File(tmpdir + "test.txt");
        if (!file.createNewFile())
            System.err.println("\"" + file.getCanonicalPath() + "\" could not be created.");

        final byte[] array = new byte[2 * 262144 + 20];
        new SecureRandom().nextBytes(array);
        String generatedString = new String(array, StandardCharsets.UTF_8);
        try (PrintStream out = new PrintStream(new FileOutputStream(file), false, "UTF-8")) {
            out.print(generatedString);
        }

        long id = image.addFile(client, file);

        List<FileAnnotationWrapper> files = image.getFileAnnotations(client);
        for (FileAnnotationWrapper f : files) {
            if (f.getId() == id) {
                assertEquals(file.getName(), f.getFileName());
                assertEquals("txt", f.getFileFormat());
                assertEquals("text/plain", f.getOriginalMimetype());
                assertEquals("text/plain", f.getServerFileMimetype());
                assertEquals("Plain Text Document", f.getFileKind());
                assertEquals(tmpdir, f.getContentAsString());
                assertEquals(tmpdir, f.getFilePath());
                assertFalse(f.isMovieFile());

                File uploadedFile = f.getFile(client, "." + File.separator + "uploaded.txt");

                List<String> expectedLines = Files.readAllLines(file.toPath());
                List<String> lines         = Files.readAllLines(uploadedFile.toPath());
                assertEquals(expectedLines.size(), lines.size());
                for (int i = 0; i < expectedLines.size(); i++) {
                    assertEquals(expectedLines.get(i), lines.get(i));
                }

                if (!uploadedFile.delete())
                    System.err.println("\"" + uploadedFile.getCanonicalPath() + "\" could not be deleted.");
            }
        }

        client.deleteFile(id);

        if (!file.delete())
            System.err.println("\"" + file.getCanonicalPath() + "\" could not be deleted.");

        assertNotEquals(0L, id);
    }


    @Test
    public void testGetCreated() throws Exception {
        LocalDate created = client.getImage(IMAGE1.id).getCreated().toLocalDateTime().toLocalDate();
        LocalDate now     = LocalDate.now();

        assertEquals(now, created);
    }


    @Test
    public void testGetAcquisitionDate() throws Exception {
        LocalDateTime     acq = client.getImage(IMAGE1.id).getAcquisitionDate().toLocalDateTime();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

        assertEquals("2020-04-01_20-04-01", dtf.format(acq));
    }


    @Test
    public void testGetChannel() throws Exception {
        ChannelWrapper channel = client.getImage(IMAGE1.id).getChannels(client).get(0);
        assertEquals(0, channel.getIndex());
        channel.setName("Foo channel");
        assertEquals("Foo channel", channel.getName());
    }


    @Test
    public void testSetDescription() throws Exception {
        ImageWrapper image = client.getImage(IMAGE1.id);

        String description  = image.getDescription();
        String description2 = "Foo";
        image.setDescription(description2);
        image.saveAndUpdate(client);
        assertEquals(description2, client.getImage(IMAGE1.id).getDescription());
        image.setDescription(description);
        image.saveAndUpdate(client);
        assertEquals(description, client.getImage(IMAGE1.id).getDescription());
    }


    @Test
    public void testSetName() throws Exception {
        ImageWrapper image = client.getImage(IMAGE1.id);

        String name  = image.getName();
        String name2 = "Foo image";
        image.setName(name2);
        image.saveAndUpdate(client);
        assertEquals(name2, client.getImage(IMAGE1.id).getName());
        image.setName(name);
        image.saveAndUpdate(client);
        assertEquals(name, image.getName());
    }


    @Test
    public void testGetCropFromROI() throws Exception {
        ImageWrapper image = client.getImage(IMAGE1.id);

        final RectangleWrapper rectangle = new RectangleWrapper(30, 30, 20, 20);
        rectangle.setCZT(1, 1, 2);

        final EllipseWrapper ellipse = new EllipseWrapper(50, 50, 20, 40);
        ellipse.setCZT(1, 0, 1);

        final int[] xBound = {30, 69};
        final int[] yBound = {10, 89};
        final int[] cBound = {1, 1};
        final int[] zBound = {0, 1};
        final int[] tBound = {1, 2};

        ROIWrapper roiWrapper = new ROIWrapper();
        roiWrapper.setImage(image);
        roiWrapper.addShape(rectangle);
        roiWrapper.addShape(ellipse);

        ImagePlus imp1 = image.toImagePlus(client, roiWrapper);
        ImagePlus imp2 = image.toImagePlus(client, xBound, yBound, cBound, zBound, tBound);

        ImageCalculator calculator = new ImageCalculator();
        ImagePlus       difference = calculator.run("difference create stack", imp1, imp2);
        ImageStatistics stats      = difference.getStatistics();

        assertEquals(0, (int) stats.max);
        assertEquals(imp1.getWidth(), imp2.getWidth());
        assertEquals(imp1.getHeight(), imp2.getHeight());
        assertEquals(imp1.getNChannels(), imp2.getNChannels());
        assertEquals(imp1.getNSlices(), imp2.getNSlices());
        assertEquals(imp1.getNFrames(), imp2.getNFrames());
    }


    @Test
    public void testGetThumbnail() throws Exception {
        final int size = 96;

        ImageWrapper  image     = client.getImage(IMAGE1.id);
        BufferedImage thumbnail = image.getThumbnail(client, size);
        assertNotNull(thumbnail);
        assertEquals(size, thumbnail.getWidth());
        assertEquals(size, thumbnail.getHeight());
    }


    @Test
    public void testDownload() throws Exception {
        ImageWrapper image = client.getImage(IMAGE1.id);
        List<File>   files = image.download(client, ".");
        assertEquals(2, files.size());
        assertTrue(files.get(0).exists());
        Files.deleteIfExists(files.get(0).toPath());
        Files.deleteIfExists(files.get(1).toPath());
    }


    @Test
    public void testImportAndRenameImages() throws Exception {
        String filename = "8bit-unsigned&pixelType=uint8&sizeZ=5&sizeC=5&sizeT=7&sizeX=512&sizeY=512.fake";

        DatasetWrapper dataset = new DatasetWrapper("Test Import & Replace", "");
        client.getProject(PROJECT1.id).addDataset(client, dataset);

        File imageFile = new File("." + File.separator + filename);
        if (!imageFile.createNewFile())
            System.err.println("\"" + imageFile.getCanonicalPath() + "\" could not be created.");

        File file = new File("." + File.separator + "test.txt");
        if (!file.createNewFile())
            System.err.println("\"" + file.getCanonicalPath() + "\" could not be created.");

        final byte[] array = new byte[2 * 262144 + 20];
        new SecureRandom().nextBytes(array);
        String generatedString = new String(array, StandardCharsets.UTF_8);
        try (PrintStream out = new PrintStream(new FileOutputStream(file), false, "UTF-8")) {
            out.print(generatedString);
        }

        List<Long>   ids1   = dataset.importImage(client, imageFile.getAbsolutePath());
        ImageWrapper image1 = client.getImage(ids1.get(0));
        image1.setDescription("This is");
        image1.saveAndUpdate(client);

        TagAnnotationWrapper tag1 = new TagAnnotationWrapper(client, "ReplaceTestTag1", "Copy annotations");
        image1.addTag(client, tag1);
        image1.addPairKeyValue(client, "Map", "ReplaceTest");

        long fileId = image1.addFile(client, file);
        if (!file.delete())
            System.err.println("\"" + file.getCanonicalPath() + "\" could not be deleted.");
        assertNotEquals(0L, fileId);

        List<Long>   ids2   = dataset.importImage(client, imageFile.getAbsolutePath());
        ImageWrapper image2 = client.getImage(ids2.get(0));
        image2.setDescription("a test.");
        image2.saveAndUpdate(client);

        TagAnnotationWrapper tag2 = new TagAnnotationWrapper(client, "ReplaceTestTag2", "Copy annotations");
        image2.addTag(client, tag2);
        image2.addFileAnnotation(client, image1.getFileAnnotations(client).get(0));
        image2.addMapAnnotation(client, image1.getMapAnnotations(client).get(0));

        final RectangleWrapper rectangle = new RectangleWrapper(30, 30, 20, 20);
        ROIWrapper roi = new ROIWrapper();
        roi.setImage(image2);
        roi.addShape(rectangle);
        image2.saveROI(client, roi);

        FolderWrapper folder = new FolderWrapper(client, "ReplaceTestFolder");
        folder.setImage(image2);
        folder.addROI(client, roi);

        TableWrapper table = new TableWrapper(1, "ReplaceTestTable");
        table.setColumn(0, "Name", String.class);
        table.setRowCount(1);
        table.addRow("Annotation");
        image1.addTable(client, table);
        image2.addTable(client, table);

        List<Long>   ids3   = dataset.importAndReplaceImages(client, imageFile.getAbsolutePath());
        ImageWrapper image3 = client.getImage(ids3.get(0));

        assertEquals(2, image3.getTags(client).size());
        assertEquals(2, image3.getTables(client).size());
        assertEquals(3, image3.getFileAnnotations(client).size());
        assertEquals(1, image3.getMapAnnotations(client).size());
        assertEquals(1, image3.getROIs(client).size());
        assertEquals(1, image3.getFolders(client).size());
        assertEquals("ReplaceTestTag1", image3.getTags(client).get(0).getName());
        assertEquals("ReplaceTestTag2", image3.getTags(client).get(1).getName());
        assertEquals("ReplaceTest", image3.getValue(client, "Map"));
        assertEquals("ReplaceTestTable", image3.getTables(client).get(0).getName());
        assertEquals("This is\na test.", image3.getDescription());

        client.delete(image3.getMapAnnotations(client).get(0));

        if (!imageFile.delete())
            System.err.println("\"" + imageFile.getCanonicalPath() + "\" could not be deleted.");

        List<ImageWrapper> images = dataset.getImages(client);

        for (ImageWrapper image : images) {
            client.delete(image);
        }
        List<ImageWrapper> endImages = dataset.getImages(client);
        client.delete(dataset);
        client.delete(tag1);
        client.delete(tag2);
        client.delete(table);
        client.deleteFile(fileId);
        client.delete(roi);
        client.delete(folder);

        assertEquals(1, images.size());
        assertTrue(endImages.isEmpty());
    }


}
