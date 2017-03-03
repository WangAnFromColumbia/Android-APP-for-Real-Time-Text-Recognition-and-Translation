package com.dynamsoft.tessocr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.widget.Toast;


import java.util.Locale;

import android.graphics.Bitmap.Config;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Scalar;


public class OCRActivity extends Activity implements OnClickListener, OnInitListener {
	private TessOCR mTessOCR;
	private TextView mResult;
	private TextView mLanguage;
	private ProgressDialog mProgressDialog;
	private ImageView mImage;
	private Button mButtonGallery, mButtonCamera, mButtonLanguage, mButtonStoptts;
	private String mCurrentPhotoPath;
	private static final int REQUEST_TAKE_PHOTO = 1;
	private static final int REQUEST_PICK_PHOTO = 2;
	private TextToSpeech textToSpeech;
	private String lan = new String("chi_sim");

	private static final String BaiduTrans = "http://api.fanyi.baidu.com/api/trans/vip/translate";
	private static final String APP_ID = "20160520000021516";
	private static final String key = "otPwHzMQBBZH9P4dNlTw";
	private static final String salt = "1435660288";


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


		setContentView(R.layout.activity_main);

		mResult = (TextView) findViewById(R.id.tv_result);
		mLanguage = (TextView) findViewById(R.id.tv_language);
		mLanguage.setText("语言类型：中文");
		mImage = (ImageView) findViewById(R.id.image);
		mButtonGallery = (Button) findViewById(R.id.bt_gallery);
		mButtonGallery.setOnClickListener(this);
		mButtonCamera = (Button) findViewById(R.id.bt_camera);
		mButtonCamera.setOnClickListener(this);
		mButtonLanguage = (Button) findViewById(R.id.bt_changelan);
		mButtonLanguage.setOnClickListener(this);
		mButtonStoptts = (Button) findViewById(R.id.bt_stop_tts);


		textToSpeech = new TextToSpeech(this, this); // 参数Context,TextToSpeech.OnInitListener

		//onClick -> pickPhoto() -> startActivityForResult-> onActivityResult -> uriOCR(uri) -> doOCR(bitmap)
		//onClick -> takePhoto() -> dispatchTakePictureIntent() -> startActivityForResult -> onActivityResult -> setPic() -> doOCR(bitmap)
	}

	/**
	 * 用来初始化TextToSpeech引擎
	 * status:SUCCESS或ERROR这2个值
	 * setLanguage设置语言，帮助文档里面写了有22种
	 * TextToSpeech.LANG_MISSING_DATA：表示语言的数据丢失。
	 * TextToSpeech.LANG_NOT_SUPPORTED:不支持
	 */
	@Override
	public void onInit(int status) {
		if (status == TextToSpeech.SUCCESS) {
			int result = textToSpeech.setLanguage(Locale.US);//这里将 US 改为 CHINESE ####
			if (result == TextToSpeech.LANG_MISSING_DATA
					|| result == TextToSpeech.LANG_NOT_SUPPORTED) {
				Toast.makeText(this, "数据丢失或不支持", Toast.LENGTH_SHORT).show();
			}
		}
	}

	private BaseLoaderCallback loaderCallback = new BaseLoaderCallback(this) {
		public void onManagerConnected(int status) {
			switch (status) {
				case LoaderCallbackInterface.SUCCESS:
					//System.loadLibrary("image_proc");//这个名字完全是要自己写的!! jni文件夹都没有！！！
					break;
				case LoaderCallbackInterface.INIT_FAILED:
					break;
				case LoaderCallbackInterface.INSTALL_CANCELED:
					break;
				case LoaderCallbackInterface.MARKET_ERROR:
					break;
				case LoaderCallbackInterface.INCOMPATIBLE_MANAGER_VERSION:
					break;
				default:
					super.onManagerConnected(status);
					break;
			}

		}
	};

	private void uriOCR(Uri uri) {
		if (uri != null) {
			InputStream is = null;
			try {
				is = getContentResolver().openInputStream(uri);
				BitmapFactory.Options options=new BitmapFactory.Options();
				//Bitmap bitmap = BitmapFactory.decodeStream(is);
				//加两行
				options.inJustDecodeBounds = false;   //若为false，则能直接返回bitmap,若为true,则只能得到图片大小等信息
				options.inSampleSize = 2;   //width，hight设为原来的十分一
				Bitmap bitmap = BitmapFactory.decodeStream(is,null,options);//但是这样牺牲了图片的质量，不可取

				/**
				 * 这里是OpenCV内容
				 */

				Mat rgbMat = new Mat(bitmap.getWidth(),bitmap.getHeight(), CvType.CV_8UC1);
				Mat grayMat = new Mat(bitmap.getWidth(),bitmap.getHeight(), CvType.CV_8UC1);
				Mat binaryMat = new Mat(bitmap.getWidth(),bitmap.getHeight(), CvType.CV_8UC1);
				Mat jiandan_binaryMat = new Mat(bitmap.getWidth(),bitmap.getHeight(), CvType.CV_8UC1);
				Mat binaryMat_filterd = new Mat(bitmap.getWidth(),bitmap.getHeight(), CvType.CV_8UC1);
				Mat eroded = new Mat(bitmap.getWidth(),bitmap.getHeight(), CvType.CV_8UC1);
				Mat after_Canny = new Mat(bitmap.getWidth(),bitmap.getHeight(), CvType.CV_8UC1);
				Mat rotated = new Mat(bitmap.getWidth(),bitmap.getHeight(), CvType.CV_8UC1);
				Bitmap bitmap_result = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);//创建一个bitmap
				Bitmap bitmap_show = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);//创建一个bitmap

				Utils.bitmapToMat(bitmap, rgbMat);//convert original bitmap to Mat, R G B. //获取lena彩色图像所对应的像素数据

				//灰度图
				Imgproc.cvtColor(rgbMat, grayMat, Imgproc.COLOR_RGB2GRAY);//rgbMat to gray grayMat //将彩色图像数据转换为灰度图像数据并存储到grayMat中

				//自适应二值化
				//Imgproc.threshold(grayMat, jiandan_binaryMat, 80 ,255 , Imgproc.THRESH_BINARY);
				Imgproc.adaptiveThreshold(grayMat, binaryMat, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, 5);//最后两个参数看看怎么设置比较好

				//中值滤波
				Imgproc.medianBlur(binaryMat, binaryMat_filterd, 5);

				//腐蚀
				Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(20, 20), new Point(-1, -1));
				Imgproc.erode(binaryMat_filterd, eroded, element);//腐蚀
				//Imgproc.medianBlur(grayMat, grayMat, 5);//腐蚀后有太多杂志点，中值滤波除去 好像没有用，暂时放弃

				//边缘检测
				Imgproc.Laplacian(eroded, jiandan_binaryMat, eroded.depth());
				Imgproc.Canny(eroded, after_Canny, 1, 0);

				//Huogh变化得直线，并计算得到图像倾斜角度 theta
				Mat mLines = new Mat();//检测到的直线集合
				Imgproc.HoughLines(after_Canny, mLines, 1, Math.PI / 180, 50, 0, 0, 0, Math.PI );

				double[] data;
				double rho, theta = 0;
				Point pt1 = new Point();
				Point pt2 = new Point();
				double a, b;
				double x0, y0;

				for (int i = 0; i < mLines.cols(); i++)
				{
					data = mLines.get(0, i);
					rho = data[0];
					theta = data[1];//这里得到的角度是 弧度制。。。
					a = Math.cos(theta);
					b = Math.sin(theta);
					x0 = a * rho;
					y0 = b * rho;
					pt1.x = Math.round(x0 + 1500 * (-b));
					pt1.y = Math.round(y0 + 1500 * a);
					pt2.x = Math.round(x0 - 1500 * (-b));
					pt2.y = Math.round(y0 - 1500 * a);
					//Imgproc.line(rgbMat, pt1, pt2, new Scalar(255, 0, 0), 5);
					//mResult.setText(Double.toString(rho) + "  " + Double.toString(theta * 180 / Math.PI));
				}

				//图像旋转
				Point center = new Point(rgbMat.cols() / 2, rgbMat.rows() / 2);
				Mat rotMatS = Imgproc.getRotationMatrix2D(center, (theta + Math.PI*1.5)* 180 / Math.PI, 1.0);//这里的角度是 360°制。。。。
				Imgproc.warpAffine(binaryMat, rotated, rotMatS, rgbMat.size(), 1, 0, new Scalar(255, 255, 255));

				/**
				for (int x = 0; x < mLines.cols(); x++) //只能画出一条线来，不知为什么
				{
					double[] vec = mLines.get(0, x);
					if(vec!=null) {
						double x1 = vec[0],
								y1 = vec[1],
								x2 = vec[2],
								y2 = vec[3];
						Point start = new Point(x1, y1);
						Point end = new Point(x2, y2);
						Imgproc.line(rgbMat, start, end, new Scalar(255, 0, 0), 5);
					}
				}*/
				Utils.matToBitmap(rgbMat, bitmap_show);
				Utils.matToBitmap(rotated, bitmap_result);
				//mResult.setText(output);

				mImage.setImageBitmap(bitmap_show);//bitmap 改为 grayBitmap

				doOCR(bitmap_result);

			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				if (is != null) {
					try {
						is.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}



	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();

		if (!OpenCVLoader.initDebug()) {
			//Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
			OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, loaderCallback);
		} else {
			//Log.d(TAG, "OpenCV library found inside package. Using it!");
			loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
		}

		Intent intent = getIntent();
		if (Intent.ACTION_SEND.equals(intent.getAction())) {
			Uri uri = (Uri) intent
					.getParcelableExtra(Intent.EXTRA_STREAM);
			uriOCR(uri);
		}
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();

		mTessOCR.onDestroy();
		textToSpeech.stop(); // 不管是否正在朗读TTS都被打断
		textToSpeech.shutdown(); // 关闭，释放资源
	}

	private void dispatchTakePictureIntent() {
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		// Ensure that there's a camera activity to handle the intent
		if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
			// Create the File where the photo should go
			File photoFile = null;
			try {
				photoFile = createImageFile();
			} catch (IOException ex) {
				// Error occurred while creating the File

			}
			// Continue only if the File was successfully created
			if (photoFile != null) {
				takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
						Uri.fromFile(photoFile));
				startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
			}
		}
	}

	/**
	 * http://developer.android.com/training/camera/photobasics.html
	 */
	private File createImageFile() throws IOException {
		// Create an image file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
				.format(new Date());
		String imageFileName = "JPEG_" + timeStamp + "_";
		String storageDir = Environment.getExternalStorageDirectory()
				+ "/TessOCR";
		File dir = new File(storageDir);
		if (!dir.exists())
			dir.mkdir();

		File image = new File(storageDir + "/" + imageFileName + ".jpg");

		// Save a file: path for use with ACTION_VIEW intents
		mCurrentPhotoPath = image.getAbsolutePath();
		return image;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		if (requestCode == REQUEST_TAKE_PHOTO
				&& resultCode == Activity.RESULT_OK) {
			setPic();
		}
		else if (requestCode == REQUEST_PICK_PHOTO
				&& resultCode == Activity.RESULT_OK) {
			Uri uri = data.getData();
			if (uri != null) {
				uriOCR(uri);
			}
		}
	}

	private void setPic() {
		// Get the dimensions of the View
		int targetW = mImage.getWidth();
		int targetH = mImage.getHeight();

		// Get the dimensions of the bitmap
		BitmapFactory.Options bmOptions = new BitmapFactory.Options();
		bmOptions.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
		int photoW = bmOptions.outWidth;
		int photoH = bmOptions.outHeight;

		// Determine how much to scale down the image
		int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

		// Decode the image file into a Bitmap sized to fill the View
		bmOptions.inJustDecodeBounds = false;
		bmOptions.inSampleSize = scaleFactor << 1;

		bmOptions.inSampleSize = 2; //这里直接改成2
		bmOptions.inPurgeable = true;

		Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);

		/**
		 * 这里是OpenCV内容
		 */

		Mat rgbMat = new Mat(bitmap.getWidth(),bitmap.getHeight(), CvType.CV_8UC1);
		Mat grayMat = new Mat(bitmap.getWidth(),bitmap.getHeight(), CvType.CV_8UC1);
		Mat binaryMat = new Mat(bitmap.getWidth(),bitmap.getHeight(), CvType.CV_8UC1);
		Mat binaryMat_filterd = new Mat(bitmap.getWidth(),bitmap.getHeight(), CvType.CV_8UC1);
		Mat eroded = new Mat(bitmap.getWidth(),bitmap.getHeight(), CvType.CV_8UC1);
		Mat after_Canny = new Mat(bitmap.getWidth(),bitmap.getHeight(), CvType.CV_8UC1);
		Mat rotated = new Mat(bitmap.getWidth(),bitmap.getHeight(), CvType.CV_8UC1);
		Bitmap bitmap_result = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);//创建一个bitmap
		Bitmap bitmap_show = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);//创建一个bitmap
		Utils.bitmapToMat(bitmap, rgbMat);//convert original bitmap to Mat, R G B. //获取lena彩色图像所对应的像素数据

		//灰度图
		Imgproc.cvtColor(rgbMat, grayMat, Imgproc.COLOR_RGB2GRAY);//rgbMat to gray grayMat //将彩色图像数据转换为灰度图像数据并存储到grayMat中

		//自适应二值化
		Imgproc.adaptiveThreshold(grayMat, binaryMat, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 35, 10);//最后两个参数看看怎么设置比较好

		//中值滤波
		Imgproc.medianBlur(binaryMat, binaryMat_filterd, 5);

		//腐蚀
		Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(20, 20), new Point(-1, -1));
		Imgproc.erode(binaryMat_filterd, eroded, element);//腐蚀
		//Imgproc.medianBlur(grayMat, grayMat, 5);//腐蚀后有太多杂志点，中值滤波除去 好像没有用，暂时放弃

		//边缘检测
		Imgproc.Canny(eroded, after_Canny, 1, 0);

		//Huogh变化得直线，并计算得到图像倾斜角度 theta
		Mat mLines = new Mat();//检测到的直线集合
		Imgproc.HoughLines(after_Canny, mLines, 1, Math.PI / 180, 50, 0, 0, 0, Math.PI );

		double[] data;
		double rho, theta = 0;
		Point pt1 = new Point();
		Point pt2 = new Point();
		double a, b;
		double x0, y0;

		for (int i = 0; i < mLines.cols(); i++)
		{
			data = mLines.get(0, i);
			rho = data[0];
			theta = data[1];//这里得到的角度是 弧度制。。。
			a = Math.cos(theta);
			b = Math.sin(theta);
			x0 = a * rho;
			y0 = b * rho;
			pt1.x = Math.round(x0 + 1500 * (-b));
			pt1.y = Math.round(y0 + 1500 * a);
			pt2.x = Math.round(x0 - 1500 * (-b));
			pt2.y = Math.round(y0 - 1500 * a);
			//Imgproc.line(rgbMat, pt1, pt2, new Scalar(255, 0, 0), 5);
			//mResult.setText(Double.toString(rho) + "  " + Double.toString(theta * 180 / Math.PI));
		}

		//图像旋转
		Point center = new Point(rgbMat.cols() / 2, rgbMat.rows() / 2);
		Mat rotMatS = Imgproc.getRotationMatrix2D(center, (theta + Math.PI*1.5)* 180 / Math.PI, 1.0);//这里的角度是 360°制。。。。
		Imgproc.warpAffine(binaryMat, rotated, rotMatS, rgbMat.size(), 1, 0, new Scalar(255, 255, 255));

		/**
		 for (int x = 0; x < mLines.cols(); x++) //只能画出一条线来
		 {
		 double[] vec = mLines.get(0, x);
		 if(vec!=null) {
		 double x1 = vec[0],
		 y1 = vec[1],
		 x2 = vec[2],
		 y2 = vec[3];
		 Point start = new Point(x1, y1);
		 Point end = new Point(x2, y2);
		 Imgproc.line(rgbMat, start, end, new Scalar(255, 0, 0), 5);
		 }
		 }*/

		Utils.matToBitmap(rgbMat, bitmap_show);
		Utils.matToBitmap(rotated, bitmap_result);
		//mResult.setText(output);


		mImage.setImageBitmap(bitmap_show);//bitmap 改为 grayBitmap

		doOCR(bitmap_result);

	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		int id = v.getId();
		switch (id) {
			case R.id.bt_gallery:
				pickPhoto();
				break;
			case R.id.bt_camera:
				takePhoto();
				break;
			case R.id.bt_changelan:
				if (lan.equals("chi_sim")) {
					lan = "eng";
					mTessOCR = new TessOCR(lan);
					mLanguage.setText("Language type: English");
				} else {
					lan = "chi_sim";
					mTessOCR = new TessOCR(lan);
					mLanguage.setText("语言类型：中文");
				}
				break;
			case R.id.bt_stop_tts:
				textToSpeech.stop(); // 不管是否正在朗读TTS都被打断
				break;
		}
	}
	
	private void pickPhoto() {
		Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		startActivityForResult(intent, REQUEST_PICK_PHOTO);
	}

	private void takePhoto() {
		dispatchTakePictureIntent();
	}

	private void doOCR(final Bitmap bitmap) {
		if (mProgressDialog == null) {
			mProgressDialog = ProgressDialog.show(this, "Processing",
					"Doing OCR...", true);
		}
		else {
			mProgressDialog.show();
		}
		
		new Thread(new Runnable() {
				public void run() {

				final String result = mTessOCR.getOCRResult(bitmap);

				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						// TODO Auto-generated method stub
						if (result != null && !result.equals("")) {

							//mResult.setText(result);

							new Thread(new Runnable() {
								@Override
								public void run() {
									/** 百度翻译*/
									String translated = transEnTo(result);

									Message msg = new Message();
									msg.what = 0;
									Bundle bun = new Bundle();
									bun.putString("word", translated);//这里translated改成result
									msg.setData(bun);
									insHandler.sendMessage(msg);

								}
							}).start();


						}


						if (textToSpeech != null && !textToSpeech.isSpeaking()) {
							textToSpeech.setPitch(0.5f);// 设置音调，值越大声音越尖（女生），值越小则变成男声,1.0是常规
							textToSpeech.speak(mResult.getText().toString(),
									TextToSpeech.QUEUE_FLUSH, null);
						}

						mProgressDialog.dismiss();
					}

				});

			};
		}).start();
	}

	private Handler insHandler = new Handler() {//Handler必须为static静态？不设为static会有黄色警告
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			switch (msg.what) {
				case 0:
					String word = msg.getData().getString("word");
					mResult.setText(word);
					//mResult.setText("ssss");
					break;

				default:
					break;
			}
		}
	};

	static String transEnTo(String result) {
		String putword = result;
		String str_return = new String("nochange?");
		String str = new String();
		String sign = new String();
		String back = new String();
		StringBuilder result111 = new StringBuilder();
		//int salt = random.nextInt(10000);
//        try {
//            // 对中文字符进行编码
//            putword = URLEncoder.encode(putword, "UTF-8");
//            if (putword != null){
//                return "result: " + result + "\n" + "putword: "+ putword;
//            }
//
//        } catch (Exception e) {
//            String textchongfu = "encode 错";
//            return textchongfu;
//        }

		try {
			str = APP_ID + putword + salt + key; //！！！！
			sign = MD5Util.getMD5String(str, "utf-8");

			putword = URLEncoder.encode(putword, "UTF-8"); //把这句话果然要移后面来！！！坑爹的百度！！！

			URL url = new URL(BaiduTrans + "?q=" + putword + "&from=zh&to=en&appid=" + APP_ID
					+ "&salt=" + salt +"&sign=" + sign);
//            if (sign != null){
//                return sign;
//            }
//            if (BaiduTrans != null) {
//                return BaiduTrans + "?q=" + putword + "&from=en&to=zh&appid=" + APP_ID
//                        + "&salt=" + salt + "&sign=" + sign; //将这个url放浏览器里试试
//            }  //不用测试了，url是对的！还有问题的话就是后面的问题了！！！！



			URLConnection con = url.openConnection();
			con.connect();
			InputStreamReader reader = new InputStreamReader(con.getInputStream());//这句为什么一直错啊！！！！！！
			BufferedReader bufread = new BufferedReader(reader);
			String line;
			while ((line = bufread.readLine()) != null) {
				result111.append(line).append("\n");
			}
		} catch (Exception e) {
			String textchongfu = "第二部分错";
			return textchongfu;
		}


		try {
			JSONObject resultJson = new JSONObject(result111.toString());
//			String error_code = resultJson.getString("from");
//			if (error_code != null) {
//				return ("出错代码:" + error_code + "from:" + resultJson.getString("to"));
//			}

			JSONArray array = (JSONArray) resultJson.get("trans_result");
			String really_final_result = "";
			for (int i = 0; i < array.length(); i++){
				JSONObject dst = (JSONObject) array.get(i);
				String text = dst.getString("dst");
				text = URLDecoder.decode(text, "utf-8");
				really_final_result = really_final_result + text;
			}

			if (really_final_result != null) {
				return really_final_result;
			}

		} catch (Exception e) {
			String text1 = "第三部分错";
			return text1;
		}

		String last = "到了最后";
		return last;
	}


	/**
	 * 获取jsoon中翻译的内容
	 *
	 * @param jstring
	 * @return
	 */
	static String JsonToString(String jstring) {
		try {
			JSONObject obj = new JSONObject(jstring);
			JSONArray array = obj.getJSONArray("trans_result");
			obj = array.getJSONObject(0);
			String word = obj.getString("dst");
			return word;
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}
}
