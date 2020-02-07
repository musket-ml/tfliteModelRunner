# TensorFlow Lite Model Runner

Android application for testing TensorFlow lite models.

Now supported:
 - Classification models. Only photo classification.
 - Segmentation models. Photo and video segmentation.
 - Object detection models. Only photo object detection.

The application has a demo segmentation model.

![](/media/tf-model-runner_small.gif)

## Installation

Download and install [TFLiteModelRunner apk file](https://github.com/musket-ml/tfliteModelRunner/raw/master/apk/release/tflite_runner_release_1.01.apk) on your android device.

## Usage

Only TensorFlow light models are applicable. To convert a TensorFlow model to a TensorFlow light model see [guide.](https://www.tensorflow.org/lite/guide/get_started#2_convert_the_model_format)

### Adding a new model

1) Open app. Then click tab `ADD`
2) Select a model type.
3) Enter a title, for tab.
4) Enter image width and height dimensions for model input
5) Select a tflite file.

After file loading, a new tab with the specified name will be created.

![](/media/tf-model-runner_new_model_small.gif)

### Remove model

1) Click on the desired tab.
2) Open options menu and select `Remove current tab`

![](/media/tf-model-runner_remove_small.gif)

### Run model

1) Click on the desired tab.
2) Click on the camera button.
3) Take a photo or video. Wait for the end of processing. 

![](/media/tf-model-runner_run_small.gif)

### View results

Click on the result item.
