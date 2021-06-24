[![Java CI with Maven](https://github.com/GReD-Clermont/simple-omero-client/actions/workflows/maven.yml/badge.svg)](https://github.com/GReD-Clermont/simple-omero-client/actions/workflows/maven.yml) [![codecov](https://codecov.io/gh/GReD-Clermont/simple-omero-client/branch/main/graph/badge.svg)](https://codecov.io/gh/GReD-Clermont/simple-omero-client)

# simple-omero-client

A Maven project to easily connect to OMERO.

This library presents a simplified API to put/get objects on an OMERO server. 
<p>It can be used from Maven.
<p>The JAR can be put into the ImageJ plugins folder to use it from scripts.


## How to use:


### Connection:
The main entry point is the Client class, which can be used to retrieve, save or delete objects.

<p>To use it, a connection has to be established first:

```java
Client client=new Client();
client.connect("host", 4064, "username", password, groupId);
```

### Repository objects (projects, datasets, images)

It can then be used to retrieve all the repository objects the user has access to, like projects or datasets:

```java
List<ProjectWrapper> projects=client.getProjects();
List<DatasetWrapper> datasets=client.getDatasets();
```

These objects can then be used to retrieve their children:

```java
for(DatasetWrapper dataset:datasets){
    List<ImageWrapper> images=dataset.getImages();
    //...
}
```

### Annotations

For each type of objects (project, dataset or image), annotations can be retrieved/added, such as:

* ####Tags:

```java
TagAnnotationWrapper tag = new TagAnnotationWrapper(client, "name", "description");
dataset.addTag(tag);
List<TagAnnotationWrapper> tags = dataset.getTags();
```

* ####Key/Value pairs:

```java
dataset.addPairKeyValue("key", "value");
String value = dataset.getValue("key");
```

* #### Tables:

```java
TableWrapper table = new TableWrapper(columnCount, "name");
dataset.addTable(table);
List<TableWrapper> tables = dataset.getTables();
```

* #### Files:

```java
File file = new File("file.csv");
dataset.addFile(file);
```

### Images

Pixel intensities can be downloaded from images to a Java array or as an ImagePlus:

```java
double[][][][][] pixels = image.getPixels().getAllPixels();
ImagePlus imp = image.toImagePlus();
```

Thumbnails of the specified size can be retrieved:

```java
int size = 128;
BufferedImage thumbnail = image.getThumbnail(size);
```

### ROIs

ROIs can be added to images or retrieved from them:

```java
ROIWrapper roi = new ROIWrapper();
roi.addShape(new RectangleWrapper(0, 0, 5, 5));
roi.setImage(image);
image.saveROI(roi);
List<ROIWrapper> rois = image.getROIs();
```

They can also be converted from or to ImageJ Rois:

```java
// The property is a string used to create 3D/4D ROIs in OMERO, by grouping shapes sharing the same value
List<ROIWrapper> omeroRois = ROIWrapper.fromImageJ(client, ijRois, property);

ROIWrapper roi = new ROIWrapper(client);
roi.addShape(new RectangleWrapper(0, 0, 5, 5));
List<Roi> imagejRois = roi.toImageJ();
```

## License
[GPLv2+](https://choosealicense.com/licenses/gpl-2.0/)