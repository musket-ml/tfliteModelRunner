# TensorFlow Lite Model Runner

Android application for testing tenorflow lite models.

Now supported:
 - Classification models. Only photo classification.
 - Segmentation models. Photo and video segmentation.

The application has a demo segmentation model.

## Usage

Only TensorFlow light models are applicable. To convert a TensorFlow model to a TensorFlow light model see [guide.](https://www.tensorflow.org/lite/guide/get_started#2_convert_the_model_format)

### Adding a new model

1) Open app. Then click tab `ADD`
2) Select a model type.
3) Enter a title, for tab.
4) Enter image width and height dimensions for model input
5) Select a tflite file.

After file loading, a new tab with the specified name will be created.

### Remove model

1) Click on the desired tab.
2) Open options menu and select `Remove current tab`


### Run model

1) Click on the desired tab.
2) Click on the camera button.
3) Take a photo or video. Wait for the end of processing. 

### View results

Click on the result item.
