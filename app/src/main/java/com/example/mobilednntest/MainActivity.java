package com.example.mobilednntest;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.*;



public class MainActivity extends AppCompatActivity{
    private static final int RESULT_LOAD_IMAGE = 1;
    CameraSurfaceView cameraSurfaceView;
    private static final int SINGLE_PERMISSION = 1004; //권한 변수
    Module module_detection = null; // detection Yolo 모델
    Module module_depth = null; // depth 모델
    public Bitmap raw_bitmap;
    public boolean image_flag = true;

    private static final String TAG = "Fcw_Main";
    private static final String TAG_i = "Fcw_Main_important";
    private long detectTime = 0;
    private long depthInferenceTime = 0;
    private int inferenceFPS =1800;

    float TD = 0;

    // color map
    public double[][] colormap = {{0.18995,0.07176,0.23217},{0.19483,0.08339,0.26149},{0.19956,0.09498,0.29024},{0.20415,0.10652,0.31844},{0.20860,0.11802,0.34607},{0.21291,0.12947,0.37314},{0.21708,0.14087,0.39964},{0.22111,0.15223,0.42558},{0.22500,0.16354,0.45096},{0.22875,0.17481,0.47578},{0.23236,0.18603,0.50004},{0.23582,0.19720,0.52373},{0.23915,0.20833,0.54686},{0.24234,0.21941,0.56942},{0.24539,0.23044,0.59142},{0.24830,0.24143,0.61286},{0.25107,0.25237,0.63374},{0.25369,0.26327,0.65406},{0.25618,0.27412,0.67381},{0.25853,0.28492,0.69300},{0.26074,0.29568,0.71162},{0.26280,0.30639,0.72968},{0.26473,0.31706,0.74718},{0.26652,0.32768,0.76412},{0.26816,0.33825,0.78050},{0.26967,0.34878,0.79631},{0.27103,0.35926,0.81156},{0.27226,0.36970,0.82624},{0.27334,0.38008,0.84037},{0.27429,0.39043,0.85393},{0.27509,0.40072,0.86692},{0.27576,0.41097,0.87936},{0.27628,0.42118,0.89123},{0.27667,0.43134,0.90254},{0.27691,0.44145,0.91328},{0.27701,0.45152,0.92347},{0.27698,0.46153,0.93309},{0.27680,0.47151,0.94214},{0.27648,0.48144,0.95064},{0.27603,0.49132,0.95857},{0.27543,0.50115,0.96594},{0.27469,0.51094,0.97275},{0.27381,0.52069,0.97899},{0.27273,0.53040,0.98461},{0.27106,0.54015,0.98930},{0.26878,0.54995,0.99303},{0.26592,0.55979,0.99583},{0.26252,0.56967,0.99773},{0.25862,0.57958,0.99876},{0.25425,0.58950,0.99896},{0.24946,0.59943,0.99835},{0.24427,0.60937,0.99697},{0.23874,0.61931,0.99485},{0.23288,0.62923,0.99202},{0.22676,0.63913,0.98851},{0.22039,0.64901,0.98436},{0.21382,0.65886,0.97959},{0.20708,0.66866,0.97423},{0.20021,0.67842,0.96833},{0.19326,0.68812,0.96190},{0.18625,0.69775,0.95498},{0.17923,0.70732,0.94761},{0.17223,0.71680,0.93981},{0.16529,0.72620,0.93161},{0.15844,0.73551,0.92305},{0.15173,0.74472,0.91416},{0.14519,0.75381,0.90496},{0.13886,0.76279,0.89550},{0.13278,0.77165,0.88580},{0.12698,0.78037,0.87590},{0.12151,0.78896,0.86581},{0.11639,0.79740,0.85559},{0.11167,0.80569,0.84525},{0.10738,0.81381,0.83484},{0.10357,0.82177,0.82437},{0.10026,0.82955,0.81389},{0.09750,0.83714,0.80342},{0.09532,0.84455,0.79299},{0.09377,0.85175,0.78264},{0.09287,0.85875,0.77240},{0.09267,0.86554,0.76230},{0.09320,0.87211,0.75237},{0.09451,0.87844,0.74265},{0.09662,0.88454,0.73316},{0.09958,0.89040,0.72393},{0.10342,0.89600,0.71500},{0.10815,0.90142,0.70599},{0.11374,0.90673,0.69651},{0.12014,0.91193,0.68660},{0.12733,0.91701,0.67627},{0.13526,0.92197,0.66556},{0.14391,0.92680,0.65448},{0.15323,0.93151,0.64308},{0.16319,0.93609,0.63137},{0.17377,0.94053,0.61938},{0.18491,0.94484,0.60713},{0.19659,0.94901,0.59466},{0.20877,0.95304,0.58199},{0.22142,0.95692,0.56914},{0.23449,0.96065,0.55614},{0.24797,0.96423,0.54303},{0.26180,0.96765,0.52981},{0.27597,0.97092,0.51653},{0.29042,0.97403,0.50321},{0.30513,0.97697,0.48987},{0.32006,0.97974,0.47654},{0.33517,0.98234,0.46325},{0.35043,0.98477,0.45002},{0.36581,0.98702,0.43688},{0.38127,0.98909,0.42386},{0.39678,0.99098,0.41098},{0.41229,0.99268,0.39826},{0.42778,0.99419,0.38575},{0.44321,0.99551,0.37345},{0.45854,0.99663,0.36140},{0.47375,0.99755,0.34963},{0.48879,0.99828,0.33816},{0.50362,0.99879,0.32701},{0.51822,0.99910,0.31622},{0.53255,0.99919,0.30581},{0.54658,0.99907,0.29581},{0.56026,0.99873,0.28623},{0.57357,0.99817,0.27712},{0.58646,0.99739,0.26849},{0.59891,0.99638,0.26038},{0.61088,0.99514,0.25280},{0.62233,0.99366,0.24579},{0.63323,0.99195,0.23937},{0.64362,0.98999,0.23356},{0.65394,0.98775,0.22835},{0.66428,0.98524,0.22370},{0.67462,0.98246,0.21960},{0.68494,0.97941,0.21602},{0.69525,0.97610,0.21294},{0.70553,0.97255,0.21032},{0.71577,0.96875,0.20815},{0.72596,0.96470,0.20640},{0.73610,0.96043,0.20504},{0.74617,0.95593,0.20406},{0.75617,0.95121,0.20343},{0.76608,0.94627,0.20311},{0.77591,0.94113,0.20310},{0.78563,0.93579,0.20336},{0.79524,0.93025,0.20386},{0.80473,0.92452,0.20459},{0.81410,0.91861,0.20552},{0.82333,0.91253,0.20663},{0.83241,0.90627,0.20788},{0.84133,0.89986,0.20926},{0.85010,0.89328,0.21074},{0.85868,0.88655,0.21230},{0.86709,0.87968,0.21391},{0.87530,0.87267,0.21555},{0.88331,0.86553,0.21719},{0.89112,0.85826,0.21880},{0.89870,0.85087,0.22038},{0.90605,0.84337,0.22188},{0.91317,0.83576,0.22328},{0.92004,0.82806,0.22456},{0.92666,0.82025,0.22570},{0.93301,0.81236,0.22667},{0.93909,0.80439,0.22744},{0.94489,0.79634,0.22800},{0.95039,0.78823,0.22831},{0.95560,0.78005,0.22836},{0.96049,0.77181,0.22811},{0.96507,0.76352,0.22754},{0.96931,0.75519,0.22663},{0.97323,0.74682,0.22536},{0.97679,0.73842,0.22369},{0.98000,0.73000,0.22161},{0.98289,0.72140,0.21918},{0.98549,0.71250,0.21650},{0.98781,0.70330,0.21358},{0.98986,0.69382,0.21043},{0.99163,0.68408,0.20706},{0.99314,0.67408,0.20348},{0.99438,0.66386,0.19971},{0.99535,0.65341,0.19577},{0.99607,0.64277,0.19165},{0.99654,0.63193,0.18738},{0.99675,0.62093,0.18297},{0.99672,0.60977,0.17842},{0.99644,0.59846,0.17376},{0.99593,0.58703,0.16899},{0.99517,0.57549,0.16412},{0.99419,0.56386,0.15918},{0.99297,0.55214,0.15417},{0.99153,0.54036,0.14910},{0.98987,0.52854,0.14398},{0.98799,0.51667,0.13883},{0.98590,0.50479,0.13367},{0.98360,0.49291,0.12849},{0.98108,0.48104,0.12332},{0.97837,0.46920,0.11817},{0.97545,0.45740,0.11305},{0.97234,0.44565,0.10797},{0.96904,0.43399,0.10294},{0.96555,0.42241,0.09798},{0.96187,0.41093,0.09310},{0.95801,0.39958,0.08831},{0.95398,0.38836,0.08362},{0.94977,0.37729,0.07905},{0.94538,0.36638,0.07461},{0.94084,0.35566,0.07031},{0.93612,0.34513,0.06616},{0.93125,0.33482,0.06218},{0.92623,0.32473,0.05837},{0.92105,0.31489,0.05475},{0.91572,0.30530,0.05134},{0.91024,0.29599,0.04814},{0.90463,0.28696,0.04516},{0.89888,0.27824,0.04243},{0.89298,0.26981,0.03993},{0.88691,0.26152,0.03753},{0.88066,0.25334,0.03521},{0.87422,0.24526,0.03297},{0.86760,0.23730,0.03082},{0.86079,0.22945,0.02875},{0.85380,0.22170,0.02677},{0.84662,0.21407,0.02487},{0.83926,0.20654,0.02305},{0.83172,0.19912,0.02131},{0.82399,0.19182,0.01966},{0.81608,0.18462,0.01809},{0.80799,0.17753,0.01660},{0.79971,0.17055,0.01520},{0.79125,0.16368,0.01387},{0.78260,0.15693,0.01264},{0.77377,0.15028,0.01148},{0.76476,0.14374,0.01041},{0.75556,0.13731,0.00942},{0.74617,0.13098,0.00851},{0.73661,0.12477,0.00769},{0.72686,0.11867,0.00695},{0.71692,0.11268,0.00629},{0.70680,0.10680,0.00571},{0.69650,0.10102,0.00522},{0.68602,0.09536,0.00481},{0.67535,0.08980,0.00449},{0.66449,0.08436,0.00424},{0.65345,0.07902,0.00408},{0.64223,0.07380,0.00401},{0.63082,0.06868,0.00401},{0.61923,0.06367,0.00410},{0.60746,0.05878,0.00427},{0.59550,0.05399,0.00453},{0.58336,0.04931,0.00486},{0.57103,0.04474,0.00529},{0.55852,0.04028,0.00579},{0.54583,0.03593,0.00638},{0.53295,0.03169,0.00705},{0.51989,0.02756,0.00780},{0.50664,0.02354,0.00863},{0.49321,0.01963,0.00955},{0.47960,0.01583,0.01055}};
    private final HashMap<Integer,float[]> TDmap = new HashMap<Integer,float[]>(){{//초기값 지정
        put(0,new float[]{0.0f,0.0f}); // Human 0
        put(1,new float[]{0.0f, 0.0f}); // dummy 1
        put(2,new float[]{0.0f,0.0f}); // Car 2
        put(3,new float[]{0.0f, 0.0f}); // dummy 1
        put(4,new float[]{0.0f,0.0f}); // Truck 4
        put(5,new float[]{0.0f,0.0f}); // Bike 5
        put(6,new float[]{0.0f,0.0f}); // Motor 6
    }};
//    private final HashMap<Integer,float[]> TDmap = new HashMap<Integer,float[]>(){{//초기값 지정
//        put(0,new float[]{17f,12f}); // Human 0
//        put(1,new float[]{0.0f, 0.0f}); // dummy 1
//        put(2,new float[]{22f,9f}); // Car 2
//        put(3,new float[]{0.0f, 0.0f}); // dummy 1
//        put(4,new float[]{24f,5f}); // Truck 4
//        put(5,new float[]{6f,2f}); // Bike 5
//        put(6,new float[]{7f,2f}); // Motor 6
//    }};
    ///////////////////////////////////////////////////////////
    // hyperparam for detect //////////////////////////////////
    ///////////////////////////////////////////////////////////


    ///////////////////////////////////////////////////////////
    // hyperparam for depth ///////////////////////////////////
    ///////////////////////////////////////////////////////////
    private final float max_depth = 100f;
    private final float min_depth = 0.1f;
    private final float STEREO_SCALE_FACTOR = 5.4f;
    // Network hyperparam
    private final String detect_model = "best_yolov5s_bdd_prew.torchscript.ptl";
    private final String depth_model = "monodepth2.ptl";

    private boolean IsDispOut = true;
    private boolean IsStarted = false;
    private final int globalWidth=640;
    private final int globalHeight=192;
    private float globalScale = (float) globalWidth / globalHeight;
    private final float FoV = 77 * (float) Math.PI / 180;
    //reference : https://support.google.com/pixelphone/answer/7158570?hl=en#zippy=%2Cpixel-a

    // get depth image
    public void runDepth(Bitmap rawbitmap, ArrayList<Result> bbox_array){
        if (module_depth != null) {
            ((TextView) findViewById(R.id.depth_text)).setText("");
            Bitmap bitmap = null;
            int width = rawbitmap.getWidth();
            int height = rawbitmap.getHeight();

            if(globalScale > (float) width / height){
                int newHeight = (int)(width/globalScale);
                bitmap = Bitmap.createBitmap(rawbitmap, 0, (height-newHeight)/2, width, newHeight);
            }else{
                int newWidth = (int)(height*globalScale);
                bitmap = Bitmap.createBitmap(rawbitmap, (width-newWidth)/2, 0, newWidth, height);
            }
            bitmap = Bitmap.createScaledBitmap(bitmap, globalWidth, globalHeight, true);
            //Input Tensor
            final Tensor input = TensorImageUtils.bitmapToFloat32Tensor(
                    bitmap,
                    TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
                    TensorImageUtils.TORCHVISION_NORM_STD_RGB
//                    PrePostProcessor.NO_MEAN_RGB,
//                    PrePostProcessor.NO_STD_RGB
            );

            //Calling the forward of the model to run our input
            Long beforeDepthInference = System.currentTimeMillis();
            final Tensor output = module_depth.forward(IValue.from(input)).toTensor();
            Long afterDepthInference = System.currentTimeMillis();
            depthInferenceTime = afterDepthInference-beforeDepthInference;
            Log.i(TAG,"Depth Inference Time: "+depthInferenceTime+"ms");
            float[] deptharray = output.getDataAsFloatArray();
            if(IsDispOut) { // disparity to depth
                for (int i = 0; i < deptharray.length; i++) {
                    deptharray[i] = disp2depth(deptharray[i]);
                }
            }

            // BEV bitmap generation
//            Bitmap bevBitmap = BEV(bitmap, deptharray);
            Bitmap bevBitmap = BEV_bbox(deptharray, bbox_array);
//            Matrix sideInversion = new Matrix();
//            sideInversion.setScale(1, -1);  // 상하반전
//            final Bitmap flippedBevBitmap = Bitmap.createBitmap(bevBitmap, 0, 0,
//                    bevBitmap.getWidth(), bevBitmap.getHeight(), sideInversion, false);
//            ((ImageView) findViewById(R.id.BEV_image)).setImageBitmap((flippedBevBitmap));
            ((ImageView) findViewById(R.id.BEV_image)).setImageBitmap((bevBitmap));


//            // Depth bitmap generation
//            Bitmap depthbitmap = arrayFlotToBitmap(deptharray, globalWidth, globalHeight);
////            Bitmap finaldepthbitmap = Bitmap.createScaledBitmap(depthbitmap, width, height,true);
//            Bitmap finaldepthbitmap = Bitmap.createScaledBitmap(depthbitmap, globalWidth, globalHeight, true);
//
//            ((ImageView) findViewById(R.id.result_image)).setImageBitmap(finaldepthbitmap);
        }
        else{
            ((TextView) findViewById(R.id.depth_text)).setText("Please upload model");
        }
    }

    // depth floatarray to bitmap image
    private Bitmap arrayFlotToBitmap(float[] floatArray,int width,int height){
        // Normalize first
        float max = 0;
        for(int i=0; i< floatArray.length; i++){
            if(max < floatArray[i]) max = floatArray[i];
        }
        float[] normFloatArray = new float[floatArray.length];
        for(int i=0; i< floatArray.length; i++){
            normFloatArray[i] = floatArray[i] / (max + 0.0001f);
        }
//        Log.i(TAG, "max : "+max);

        byte alpha = (byte) 255 ;

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888) ;

        ByteBuffer byteBuffer = ByteBuffer.allocate(width*height*4) ;

        int i = 0 ;

        while (i<normFloatArray.length){
            float x = normFloatArray[i];
            int a = (int) Math.floor(x*255);
            int b = Math.min(255, a + 1);
            float f = x * 255.0f - a;
            int R = (int) ((colormap[a][0] + (colormap[b][0] - colormap[a][0]) * (double)f)*255);
            int G = (int) ((colormap[a][1] + (colormap[b][1] - colormap[a][1]) * (double)f)*255);
            int B = (int) ((colormap[a][2] + (colormap[b][2] - colormap[a][2]) * (double)f)*255);
            byteBuffer.put(4*i, (byte) R) ;
            byteBuffer.put(4*i+1, (byte) G) ;
            byteBuffer.put(4*i+2, (byte) B) ;
            byteBuffer.put(4*i+3, alpha) ;
            i++ ;
        }
        bmp.copyPixelsFromBuffer(byteBuffer) ;
        return bmp ;
    }

    // get bird eye view arrays
    private Bitmap BEV(Bitmap originBitmap, float[] depth){
        int width = 600; // cm level -> 6m
        int height = 1500; // cm level -> 15m
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        int raw_width = originBitmap.getWidth();
        int raw_height = originBitmap.getHeight();
        int u0 = raw_width/2, v0 = raw_height/2;
//        System.out.printf("%d,%d\n",raw_width,raw_height);
        for(int u=0; u<raw_width; u++){
            for(int v=0; v<raw_height; v++){
                int idx = v * raw_width + u; // termporary
                float d = depth[idx];
                float[] xyz = pixel2global(u,v,d, u0, v0);
                // original color
                int colour = originBitmap.getPixel(u, v);

                // only use x,z
                int x = (int)(xyz[0]*100) + width/2, z = (int)(xyz[2]*100); // m -> cm
                if((x>=0 && x<width) && (z>=0 && z<height)) {
//                    System.out.printf("x : %d, depth : %d\n", x, z);
                    bmp.setPixel(x, z, colour);
                }
            }
        }

        return bmp;
    }

    private Bitmap BEV_bbox(float[] depth, ArrayList<Result> bbox_array){
        int width = 600; // cm level -> 6m
        int height = 1500; // cm level -> 15m
        int offset_x = 0, offset_y = (640-192)/2;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bitmap);
        Paint red = new Paint();
        red.setColor(Color.RED);
        Paint blue = new Paint();
        blue.setColor(Color.BLUE);
        Paint black = new Paint();
        black.setColor(Color.BLACK);
        Paint yellow = new Paint();
        yellow.setColor(Color.YELLOW);
        Paint green = new Paint();
        green.setColor(Color.GREEN);
        c.drawRect(width/2, 0, width/2+100, 200, blue);
        c.drawLine(0,0,0, height-1, black);
        c.drawLine(0,height-1, width-1, height-1, black);
        c.drawLine(width-1,height-1,width-1,0, black);
        c.drawLine(width-1,0,0,0, black);

        int result=0;
        for (int i=0;i<bbox_array.size();i++){
            int center_x = Math.min(Math.max(bbox_array.get(i).rect.centerX() - offset_x,offset_x),globalWidth-1);
            int center_y = Math.min(Math.max(bbox_array.get(i).rect.centerY() - offset_y,0),globalHeight-1);
//            Log.i(TAG, "Center idx : "+center_x+","+center_y);
            float d = depth[center_x + center_y * globalWidth];
            Log.i(TAG_i, "Depth : "+d);
            float[] xyz = pixel2global(center_x, center_y, d, 320, 96);
            int x = (int) (xyz[0]*100) + width/2, z = (int) (xyz[2]*100);

//            Log.i(TAG, "X,Z value : "+x+","+z);
            if(z <= 250){
                c.drawRect(x-50, z-100, x+50, z+100, red);
                result=2;
            }else if(z <= 500){
                result = (result>1)?result:1;
                c.drawRect(x-50, z-100, x+50, z+100, yellow);
            }
            else{
                c.drawRect(x-50, z-100, x+50, z+100, green);
            }
        }
        if(result==1){
            ((ImageView) findViewById(R.id.result_image)).setBackgroundColor(Color.YELLOW);
        }else if(result == 2){
            ((ImageView) findViewById(R.id.result_image)).setBackgroundColor(Color.RED);
        }
        else{
            ((ImageView) findViewById(R.id.result_image)).setBackgroundColor(Color.GREEN);
        }
//        bitmap = Bitmap.createScaledBitmap(bitmap, 60,150,true);
        return bitmap;
    }

    // disparity to depth calculation
    private float disp2depth(float disp){
        float min_disp = 1 / max_depth, max_disp = 1 / min_depth;
        float scaled_disp = min_disp + (max_disp - min_disp) * disp;
        return 1 / scaled_disp * STEREO_SCALE_FACTOR;
    }

    // pixel 2 global
    private float[] pixel2global(int u, int v, float depth, int u0, int v0){
        float f = (float) (u0 / Math.atan(FoV/2));
        if(globalScale < 16.0/9){
            f = (float) (v0 * 16 / 9 / Math.atan(FoV/2));
        }
        // cam spec reference : https://www.dxomark.com/google-pixel-4a-camera-review-excellent-single-camera-smartphone/
        float [] res = {(u-u0) * depth / f, (v-v0) * depth / f, depth};

        return res;
    }

    // Run Detection
    public ArrayList<Result> runDetection(Bitmap rawbitmap){
        if (module_detection != null) {
            //Read the image as Bitmap
            Bitmap bitmap = null;
            int width = rawbitmap.getWidth();
            int height = rawbitmap.getHeight();

            // 640*192
//            if(globalScale > (float) width / height){
//                int newHeight = (int)(width/globalScale);
//                bitmap = Bitmap.createBitmap(rawbitmap, 0, (height-newHeight)/2, width, newHeight);
//            }else{
//                int newWidth = (int)(height*globalScale);
//                bitmap = Bitmap.createBitmap(rawbitmap, (width-newWidth)/2, 0, newWidth, height);
//            }
//            bitmap = Bitmap.createScaledBitmap(bitmap, globalWidth, globalHeight, true);
//
//            //Input Tensor
//            final Tensor input = TensorImageUtils.bitmapToFloat32Tensor(
//                    bitmap,
//                    TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
//                    TensorImageUtils.TORCHVISION_NORM_STD_RGB
//            );

            // 640 x 640
            bitmap = Bitmap.createBitmap(width,width, Bitmap.Config.ARGB_8888);
            Canvas can = new Canvas(bitmap);
            can.drawColor(Color.BLACK);
            can.drawBitmap(rawbitmap, 0, (width-height)/2, null);
            bitmap = Bitmap.createScaledBitmap(bitmap, 640, 640, true);
//            ((ImageView) findViewById(R.id.result_image)).setImageBitmap(bitmap);

            //Input Tensor
            final Tensor input = TensorImageUtils.bitmapToFloat32Tensor(
                    bitmap,
                    PrePostProcessor.NO_MEAN_RGB,
                    PrePostProcessor.NO_STD_RGB
            );

            Log.i(TAG, "Before detection inference depth");
            //Calling the forward of the model to run our input
            Long beforeDetectInference = System.currentTimeMillis();

            IValue[] outputTuple = module_detection.forward(IValue.from(input)).toTuple();
            final Tensor outputTensor = outputTuple[0].toTensor();
            final float[] outputs = outputTensor.getDataAsFloatArray();
            final ArrayList<Result> pre_bbox_array =  PrePostProcessor.outputsToNMSPredictions(outputs, 1, 1, 1, 1, 0, 0);
            Long afterDetectInference = System.currentTimeMillis();
            detectTime = afterDetectInference-beforeDetectInference;
            Log.i(TAG,"Detect Inference Time: "+detectTime+"ms");

            final ArrayList<Result> bbox_array = new ArrayList<Result>();
            for(int i = 0 ; i < pre_bbox_array.size() ; i++) {
                int idx = pre_bbox_array.get(i).classIndex;
                if (idx == 0 || idx == 2 || idx == 4 || idx == 5 || idx == 6) {
                    bbox_array.add(pre_bbox_array.get(i));
                }
            }

            //Writing the detected class in to the text view of the layout
            PixelDecision1(rawbitmap,bbox_array);
//            ImageView warningImageView = findViewById(R.id.result_image);
//            warningImageView.setImageBitmap(bitmap);

            Log.i(TAG,"BEGIN LOG");
            for(int i = 0 ; i < bbox_array.size() ; i++) {
                Result r = bbox_array.get(i);
                Log.i(TAG,"Class: " + r.classIndex + ", ");
                Log.i(TAG,"Score: " + r.score + ", ");
                Log.i(TAG,"Rect: (left: " + r.rect.left + " top: " + r.rect.top + " right: " + r.rect.right + " bottom: " + r.rect.bottom+")");
            }
            Log.i(TAG,"END LOG");

            return bbox_array;
        }
        else{
            Log.i(TAG, "upload model please");
            return null;
        }
    }

    //
    private void PixelDecision1(Bitmap rawbitmap, ArrayList<Result> bbox_list){
        int check_x_min = 0, check_x_max = 640;
        int check_y_min = (640-360)/2, check_y_max = check_y_min+360;
        int check_zone = (check_x_max-check_x_min) * (check_y_max-check_y_min);

        int result = 0; // 0 : Low-threat, 1 : Mid-threat, 2 : High-threat
        for(int i=0;i<bbox_list.size(); i++){
            Result bbox = bbox_list.get(i);
            int center_x = bbox.rect.centerX(), center_y = bbox.rect.centerY();
            int bbox_size = bbox.rect.width() * bbox.rect.height();

            // 1. check zone 에 있는지 filtering
            if(center_x>=check_x_min && center_x<=check_x_max && center_y>=check_y_min && center_y<=check_y_max){
                // 2. Thread Degree(TD) 계산
                TD = (float)bbox_size / check_zone * 100;
                Log.i(TAG_i, "TD : "+TD);
                // 3. case별 계산
                float x1 = TDmap.get(bbox.classIndex)[0], x2 = TDmap.get(bbox.classIndex)[1];


                if(TD > x1){
                    // Run Depthestimation
//                    Log.i(TAG, "class : "+bbox.classIndex+" TD: "+2);
                    result = 2;
                    break;

                }
                else if(TD > x2){
                    // High framerate object detection
//                    Log.i(TAG, "class : "+bbox.classIndex+" TD: "+1);
                    result = 1;
                }
                else {
                    // Low framerate object detection
                    Log.i(TAG, "class : " + bbox.classIndex + " TD: " + 0);
                }
            }
        }
        Log.i(TAG_i,"detection !!!!!"+bbox_list.size());
        ImageView warningImageView = findViewById(R.id.result_image);
        if(result == 2){
            runDepth(rawbitmap, bbox_list);
        }else if(result == 1){
            warningImageView.setBackgroundColor(Color.GREEN);
            draw_ego();
        }
        else{
            warningImageView.setBackgroundColor(Color.LTGRAY);
            draw_ego();
        }
    }
    private void draw_ego(){
        int width = 600; // cm level -> 6m
        int height = 1500; // cm level -> 15m
        int offset_x = 0, offset_y = (640-192)/2;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bitmap);
        Paint blue = new Paint();
        blue.setColor(Color.BLUE);
        Paint black = new Paint();
        black.setColor(Color.BLACK);
        c.drawRect(width/2, 0, width/2+100, 200, blue);
        c.drawLine(0,0,0, height-1, black);
        c.drawLine(0,height-1, width-1, height-1, black);
        c.drawLine(width-1,height-1,width-1,0, black);
        c.drawLine(width-1,0,0,0, black);

        ((ImageView) findViewById(R.id.BEV_image)).setImageBitmap((bitmap));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) { // on수Create 가 가장 먼저 실행되는 함
        super.onCreate(savedInstanceState);
        //권한이 있는지 확인
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {//권한없음
            //권한 요청 코드
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, SINGLE_PERMISSION);
        } else {//권한있음

            /*..권한이 있는경우 실행할 코드....*/
            setContentView(R.layout.activity_main);

            Button startButton = (Button) findViewById(R.id.start_btn);

            SurfaceView mCameraView = (SurfaceView) findViewById(R.id.cameraView);
            cameraSurfaceView = new CameraSurfaceView(this);
            cameraSurfaceView.init(mCameraView);

            try {
                Log.i(TAG, "Load Detection module");
                module_detection = LiteModuleLoader.load(fetchModelFile(MainActivity.this, detect_model));
                Log.i(TAG, "Load Depth module");
                module_depth = LiteModuleLoader.load(fetchModelFile(MainActivity.this, depth_model));
                Log.i(TAG, "Load Completed");
            } catch (IOException e) {
                finish();
            }

            startButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    if(startButton.getText().toString().equals("Start")){
                        startButton.setText("Stop");
                        BackgroundThread thread = new BackgroundThread();
                        thread.start();
                    } else {
                        startButton.setText("Start");
                    }
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //This functions return the selected image from gallery
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

//            // image 위치에 뿌려주는 것
//            ImageView imageView = (ImageView) findViewById(R.id.image);
//            imageView.setImageBitmap(BitmapFactory.decodeFile(picturePath));
//
//            //Setting the URI so we can read the Bitmap from the image
//            imageView.setImageURI(null);
//            imageView.setImageURI(selectedImage);

            // 일단 같은 이미지 뿌려줌
            ImageView resultimageView = (ImageView) findViewById(R.id.result_image);
            Bitmap bitmap = BitmapFactory.decodeFile(picturePath);
            resultimageView.setImageBitmap(bitmap);
            Log.i(TAG, "getHeight(): "+bitmap.getHeight()+", getWidth: "+ bitmap.getWidth());

            //Setting the URI so we can read the Bitmap from the image
            resultimageView.setImageURI(null);
            resultimageView.setImageURI(selectedImage);

        }
    }

    // Pytorch model fetch
    public static String fetchModelFile(Context context, String modelName) throws IOException {
        File file = new File(context.getFilesDir(), modelName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(modelName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }

    //권한 요청에 대한 결과 처리
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case SINGLE_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    /*권한이 있는경우 실행할 코드....*/
                } else {
                    // 하나라도 거부한다면.
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
                    alertDialog.setTitle("앱 권한");
                    alertDialog.setMessage("해당 앱의 원할한 기능을 이용하시려면 애플리케이션 정보>권한> 에서 모든 권한을 허용해 주십시오");
                    // 권한설정 클릭시 이벤트 발생
                    alertDialog.setPositiveButton("권한설정",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
                                    startActivity(intent);
                                    dialog.cancel();
                                }
                            });
                    //취소
                    alertDialog.setNegativeButton("취소",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
                    alertDialog.show();
                }
                return;
        }

    }

    // 1.9.0 load file
    public static String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }

    final Handler handlerDetect = new Handler(){
        public void handleMessage(Message msg){
//            Bitmap bitmap = raw_bitmap;
//            Matrix sideInversion = new Matrix();
//            sideInversion.setScale(-1, 1);  // 좌우반전
//            Bitmap inputBitmap = Bitmap.createBitmap(bitmap, 0, 0,
//                    bitmap.getWidth(), bitmap.getHeight(), sideInversion, false);
//            ArrayList<Result> bbox_array = runDetection(inputBitmap);
            image_flag = false;
            Bitmap inputBitmap = raw_bitmap;
            ArrayList<Result> bbox_array = runDetection(inputBitmap);
            image_flag = true;
        }
    };

    class BackgroundThread extends Thread {
        boolean isRun = false;

        public void run() {
            isRun = true;
            while((isRun)) {

//                cameraSurfaceView.autofocus();
                Message msg = handlerDetect.obtainMessage();
                handlerDetect.sendMessage(msg);
                try {
                    Thread.sleep(inferenceFPS);// initial value for inferenceFPS = 1000 ms
                } catch (Exception e) {
                    Log.e(TAG, "Thread Run Error: "+e);
                }

            }
        }
    }
}