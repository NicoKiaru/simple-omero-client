/*
 *  Copyright (C) 2020-2021 GReD
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
import fr.igred.omero.annotations.TagAnnotationWrapper;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;


public class DatasetTest extends UserTest {


    @Test
    public void testCreateDatasetAndDeleteIt1() throws Exception {
        boolean exception = false;
        String  name      = "To delete";

        ProjectWrapper project = client.getProjects(1L).get(0);

        Long id = project.addDataset(client, name, "Dataset which will be deleted").getId();

        DatasetWrapper dataset = client.getDatasets(id).get(0);

        assertEquals(name, dataset.getName());

        assertTrue(dataset.canLink());
        assertTrue(dataset.canAnnotate());
        assertTrue(dataset.canEdit());
        assertTrue(dataset.canDelete());
        assertTrue(dataset.canChgrp());
        assertFalse(dataset.canChown());

        client.delete(dataset);

        try {
            client.getDatasets(id).iterator().next();
        } catch (NoSuchElementException e) {
            exception = true;
        }
        assertTrue(exception);
    }


    @Test
    public void testCreateDatasetAndDeleteIt2() throws Exception {
        boolean exception = false;

        ProjectWrapper project = client.getProjects(1L).get(0);

        String description = "Dataset which will be deleted";

        DatasetWrapper dataset = new DatasetWrapper("To delete", description);

        Long id = project.addDataset(client, dataset).getId();

        DatasetWrapper checkDataset = client.getDatasets(id).get(0);
        client.delete(checkDataset);

        try {
            client.getDatasets(id).iterator().next();
        } catch (NoSuchElementException e) {
            exception = true;
        }

        assertEquals(description, checkDataset.getDescription());
        assertTrue(exception);
    }


    @Test
    public void testCopyDataset() throws Exception {
        DatasetWrapper dataset = client.getDatasets(1L).get(0);

        List<ImageWrapper> images = dataset.getImages(client);

        ProjectWrapper project = client.getProjects(2L).get(0);

        String name = "Will be deleted";

        Long id = project.addDataset(client, name, "Dataset which will be deleted").getId();

        DatasetWrapper newDataset = client.getDatasets(id).get(0);

        newDataset.addImages(client, images);

        assertEquals(images.size(), newDataset.getImages(client).size());

        client.delete(newDataset);

        dataset.refresh(client);
        List<ImageWrapper> newImages = dataset.getImages(client);

        assertEquals(images.size(), newImages.size());
    }


    @Test
    public void testDatasetBasic() throws Exception {
        DatasetWrapper dataset = client.getDatasets(1L).get(0);

        assertEquals("TestDataset", dataset.getName());
        assertEquals("description", dataset.getDescription());
        assertEquals(1L, dataset.getId());
    }


    @Test
    public void testAddTagToDataset() throws Exception {
        DatasetWrapper dataset = client.getDatasets(1L).get(0);

        TagAnnotationWrapper tag = new TagAnnotationWrapper(client, "Dataset tag", "tag attached to a dataset");

        dataset.addTag(client, tag);

        List<TagAnnotationWrapper> tags = dataset.getTags(client);
        client.delete(tag);
        List<TagAnnotationWrapper> endTags = dataset.getTags(client);

        assertEquals(1, tags.size());
        assertEquals(0, endTags.size());
    }


    @Test
    public void testAddTagToDataset2() throws Exception {
        DatasetWrapper dataset = client.getDatasets(1L).get(0);

        dataset.addTag(client, "Dataset tag", "tag attached to a dataset");

        List<TagAnnotationWrapper> tags = client.getTags("Dataset tag");

        assertEquals(1, tags.size());

        client.delete(tags.get(0));
        List<TagAnnotationWrapper> endTags = client.getTags("Dataset tag");

        assertEquals(0, endTags.size());
    }


    @Test
    public void testAddTagIdToDataset() throws Exception {
        DatasetWrapper dataset = client.getDatasets(1L).get(0);

        TagAnnotationWrapper tag = new TagAnnotationWrapper(client, "Dataset tag", "tag attached to a dataset");

        dataset.addTag(client, tag.getId());

        List<TagAnnotationWrapper> tags = dataset.getTags(client);
        client.delete(tag);
        List<TagAnnotationWrapper> endTags = dataset.getTags(client);

        assertEquals(1, tags.size());
        assertEquals(0, endTags.size());
    }


    @Test
    public void testAddTagsToDataset() throws Exception {
        DatasetWrapper dataset = client.getDatasets(1L).get(0);

        TagAnnotationWrapper tag1 = new TagAnnotationWrapper(client, "Dataset tag", "tag attached to a dataset");
        TagAnnotationWrapper tag2 = new TagAnnotationWrapper(client, "Dataset tag", "tag attached to a dataset");
        TagAnnotationWrapper tag3 = new TagAnnotationWrapper(client, "Dataset tag", "tag attached to a dataset");
        TagAnnotationWrapper tag4 = new TagAnnotationWrapper(client, "Dataset tag", "tag attached to a dataset");

        dataset.addTags(client, tag1.getId(), tag2.getId(), tag3.getId(), tag4.getId());

        List<TagAnnotationWrapper> tags = dataset.getTags(client);
        client.delete(tag1);
        client.delete(tag2);
        client.delete(tag3);
        client.delete(tag4);
        List<TagAnnotationWrapper> endTags = dataset.getTags(client);

        assertEquals(4, tags.size());
        assertEquals(0, endTags.size());
    }


    @Test
    public void testAddTagsToDataset2() throws Exception {
        DatasetWrapper dataset = client.getDatasets(1L).get(0);

        TagAnnotationWrapper tag1 = new TagAnnotationWrapper(client, "Dataset tag", "tag attached to a dataset");
        TagAnnotationWrapper tag2 = new TagAnnotationWrapper(client, "Dataset tag", "tag attached to a dataset");
        TagAnnotationWrapper tag3 = new TagAnnotationWrapper(client, "Dataset tag", "tag attached to a dataset");
        TagAnnotationWrapper tag4 = new TagAnnotationWrapper(client, "Dataset tag", "tag attached to a dataset");

        dataset.addTags(client, tag1, tag2, tag3, tag4);

        List<TagAnnotationWrapper> tags = dataset.getTags(client);
        client.delete(tag1);
        client.delete(tag2);
        client.delete(tag3);
        client.delete(tag4);
        List<TagAnnotationWrapper> endTags = dataset.getTags(client);

        assertEquals(4, tags.size());
        assertEquals(0, endTags.size());
    }


    @Test
    public void testGetImagesInDataset() throws Exception {
        DatasetWrapper dataset = client.getDatasets(1L).get(0);

        List<ImageWrapper> images = dataset.getImages(client);

        assertEquals(3, images.size());
    }


    @Test
    public void testGetImagesByNameInDataset() throws Exception {
        DatasetWrapper dataset = client.getDatasets(1L).get(0);

        List<ImageWrapper> images = dataset.getImages(client, "image1.fake");

        assertEquals(2, images.size());
    }


    @Test
    public void testGetImagesLikeInDataset() throws Exception {
        DatasetWrapper dataset = client.getDatasets(1L).get(0);

        List<ImageWrapper> images = dataset.getImagesLike(client, ".fake");

        assertEquals(3, images.size());
    }


    @Test
    public void testGetImagesTaggedInDataset() throws Exception {
        DatasetWrapper dataset = client.getDatasets(1L).get(0);

        List<ImageWrapper> images = dataset.getImagesTagged(client, 1L);

        assertEquals(2, images.size());
    }


    @Test
    public void testGetImagesTaggedInDataset2() throws Exception {
        TagAnnotationWrapper tag     = client.getTag(2L);
        DatasetWrapper       dataset = client.getDatasets(1L).get(0);

        List<ImageWrapper> images = dataset.getImages(client, tag);

        assertEquals(1, images.size());
    }


    @Test
    public void testGetImagesKeyInDataset() throws Exception {
        DatasetWrapper dataset = client.getDatasets(1L).get(0);

        List<ImageWrapper> images = dataset.getImagesWithKey(client, "testKey1");

        assertEquals(3, images.size());
    }


    @Test
    public void testGetImagesPairKeyValueInDataset() throws Exception {
        DatasetWrapper dataset = client.getDatasets(1L).get(0);

        List<ImageWrapper> images = dataset.getImages(client, "testKey1", "testValue1");

        assertEquals(2, images.size());
    }


    @Test
    public void testGetImagesFromDataset() throws Exception {
        DatasetWrapper dataset = client.getDatasets(1L).get(0);

        List<ImageWrapper> images = dataset.getImages(client);
        assertEquals(3, images.size());
    }


    @Test
    public void testAddFileDataset() throws Exception {
        DatasetWrapper dataset = client.getDatasets(1L).get(0);

        File file = new File("./test.txt");
        if (!file.createNewFile())
            System.err.println("\"" + file.getCanonicalPath() + "\" could not be created.");

        byte[] array = new byte[2 * 262144 + 20];
        new Random().nextBytes(array);
        String generatedString = new String(array, StandardCharsets.UTF_8);
        try (PrintStream out = new PrintStream(new FileOutputStream("./test.txt"))) {
            out.print(generatedString);
        }

        Long id = dataset.addFile(client, file);
        if (!file.delete())
            System.err.println("\"" + file.getCanonicalPath() + "\" could not be deleted.");

        client.deleteFile(id);

        assertNotEquals(0L, id.longValue());
    }


    @Test
    public void testSetName() throws Exception {
        DatasetWrapper dataset = client.getDatasets(1L).get(0);

        String name  = dataset.getName();
        String name2 = "NewName";
        dataset.setName(name2);
        dataset.saveAndUpdate(client);
        assertEquals(name2, client.getDatasets(1L).get(0).getName());

        dataset.setName(name);
        dataset.saveAndUpdate(client);
        assertEquals(name, client.getDatasets(1L).get(0).getName());
    }


    @Test
    public void testSetDescription() throws Exception {
        DatasetWrapper dataset = client.getDatasets(1L).get(0);

        String description = dataset.getDescription();

        String description2 = "NewName";
        dataset.setDescription(description2);
        dataset.saveAndUpdate(client);
        assertEquals(description2, client.getDatasets(1L).get(0).getDescription());

        dataset.setDescription(description);
        dataset.saveAndUpdate(client);
        assertEquals(description, client.getDatasets(1L).get(0).getDescription());
    }

}
