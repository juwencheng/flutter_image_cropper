package vn.hunghd.flutter.plugins.imagecropper;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry;

import static android.app.Activity.RESULT_OK;

public class ImageCropperDelegate implements PluginRegistry.ActivityResultListener {
    private final Activity activity;
    private MethodChannel.Result pendingResult;
    private MethodCall methodCall;
    private FileUtils fileUtils;
    private Integer maxWidth=1080;
    private Integer maxHeight=1080;

    public ImageCropperDelegate(Activity activity) {
        this.activity = activity;
        fileUtils = new FileUtils();
    }

    public void startCrop(MethodCall call, MethodChannel.Result result) {
        String sourcePath = call.argument("source_path");
         maxWidth = call.argument("max_width");
         maxHeight = call.argument("max_height");
        Double ratioX = call.argument("ratio_x");
        Double ratioY = call.argument("ratio_y");
        Boolean circleShape = call.argument("circle_shape");
        String title = call.argument("toolbar_title");
        Long toolbarColor = call.argument("toolbar_color");
        Long statusBarColor = call.argument("statusbar_color");
        Long toolbarWidgetColor = call.argument("toolbar_widget_color");
        Long actionBackgroundColor = call.argument("action_background_color");
        Long actionActiveColor = call.argument("action_active_color");
        methodCall = call;
        pendingResult = result;

        File outputDir = activity.getCacheDir();
        File outputFile = new File(outputDir, "image_cropper_" + (new Date()).getTime() + ".jpg");

        Uri sourceUri = Uri.fromFile(new File(sourcePath));
        Uri destinationUri = Uri.fromFile(outputFile);
        UCrop.Options options = new UCrop.Options();
        options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
        if (circleShape) {
            options.setCircleDimmedLayer(true);
        }
        options.setCompressionQuality(90);
        if (title != null) {
            options.setToolbarTitle(title);
        }
        if (toolbarColor != null) {
            int intColor = toolbarColor.intValue();
            options.setToolbarColor(intColor);
        }
        if (statusBarColor != null) {
            int intColor = statusBarColor.intValue();
            options.setStatusBarColor(intColor);
        } else if (toolbarColor != null) {
            int intColor = toolbarColor.intValue();
            options.setStatusBarColor(darkenColor(intColor));
        }
        if (toolbarWidgetColor != null) {
            int intColor = toolbarWidgetColor.intValue();
            options.setToolbarWidgetColor(intColor);
        }
        if (actionBackgroundColor != null) {
            int intColor = actionBackgroundColor.intValue();
            options.setRootViewBackgroundColor(intColor);
        }
        if (actionActiveColor != null) {
            int intColor = actionActiveColor.intValue();
            options.setActiveControlsWidgetColor(intColor);
        }
        UCrop cropper = UCrop.of(sourceUri, destinationUri).withOptions(options);
        if (maxWidth != null && maxHeight != null) {
            cropper.withMaxResultSize(maxWidth, maxHeight);
        }
        if (ratioX != null && ratioY != null) {
            cropper.withAspectRatio(ratioX.floatValue(), ratioY.floatValue());
        }
        cropper.start(activity);
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == UCrop.REQUEST_CROP) {
            if (resultCode == RESULT_OK) {
                final Uri resultUri = UCrop.getOutput(data);
              
                Bitmap bitmap= null;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(activity.getContentResolver(),resultUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                finishWithSuccess(saveBitmap(imageScale(bitmap,maxWidth,maxHeight)));
//                finishWithSuccess(fileUtils.getPathFromUri(activity, resultUri));
                return true;
            } else if (resultCode == UCrop.RESULT_ERROR) {
                final Throwable cropError = UCrop.getError(data);
                finishWithError("crop_error", cropError.getLocalizedMessage(), cropError);
                return true;
            } else {
                pendingResult.success(null);
                clearMethodCallAndResult();
                return true;
            }
        }
        return false;
    }

    /**
     * 改变图片大小
     * @param bitmap
     * @param dst_w 指定宽度
     * @param dst_h 指定高度
     * @return
     */
    public static Bitmap imageScale(Bitmap bitmap, int dst_w, int dst_h) {
        int src_w = bitmap.getWidth();
        int src_h = bitmap.getHeight();
        float scale_w = ((float) dst_w) / src_w;
        float scale_h = ((float) dst_h) / src_h;
        Matrix matrix = new Matrix();
        matrix.postScale(scale_w, scale_h);
        Bitmap dstbmp = Bitmap.createBitmap(bitmap, 0, 0, src_w, src_h, matrix,
                true);
        return dstbmp;
    }

    /**
     * 保存图片 @param bitmap @return
     */
    private String saveBitmap(Bitmap bitmap) {
        String picPath;
        String sdCardDir = Environment.getExternalStorageDirectory() + "/DCIM";
        File appDir = new File(sdCardDir, "Camera");
        if (!appDir.exists()) appDir.mkdir();
        String fileName = System.currentTimeMillis() + ".jpg";
        File f = new File(appDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(f);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        picPath = sdCardDir + "/Camera/" + fileName;
        return picPath;
    }
    
    private void finishWithSuccess(String imagePath) {
        pendingResult.success(imagePath);
        clearMethodCallAndResult();
    }

    private void finishWithError(String errorCode, String errorMessage, Throwable throwable) {
        pendingResult.error(errorCode, errorMessage, throwable);
        clearMethodCallAndResult();
    }


    private void clearMethodCallAndResult() {
        methodCall = null;
        pendingResult = null;
    }

    private int darkenColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.8f;
        return Color.HSVToColor(hsv);
    }
}
