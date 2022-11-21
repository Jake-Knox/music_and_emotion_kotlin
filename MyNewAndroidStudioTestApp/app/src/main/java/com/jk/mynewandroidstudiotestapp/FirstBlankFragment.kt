package com.jk.mynewandroidstudiotestapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import com.jk.mynewandroidstudiotestapp.databinding.ActivityMainBinding
import com.jk.mynewandroidstudiotestapp.databinding.FragmentFirstBlankBinding
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

typealias LumaListener = (luma: Double) -> Unit

typealias FaceListener = (face: FloatArray) -> Unit

/**
 * A simple [Fragment] subclass.
 * Use the [FirstBlankFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class FirstBlankFragment : Fragment() {

    lateinit var binding : FragmentFirstBlankBinding
    lateinit var viewBinding: ActivityMainBinding
    lateinit var navController: NavController
    lateinit var myWindow: Window
    lateinit var myActivity: Activity
    lateinit var myViewModel: MyViewModel
    lateinit var myModel: MyModel

    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFirstBlankBinding.inflate(inflater,container,false)
        myWindow = requireActivity().window
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        myWindow.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        myViewModel = ViewModelProvider(requireActivity()).get(MyViewModel::class.java)
        var myModel = myViewModel.myLiveModel.value

        navController = findNavController()


        // Inflate the layout for this fragment
        //return inflater.inflate(R.layout.fragment_first_blank, container, false)

        // Request camera permissions
        if(allPermissionsGranted()){
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(), REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Set up the listeners for the take photo and video capture buttons
        binding.imageCaptureButton.setOnClickListener { takePhoto() }
        binding.videoCaptureButton.setOnClickListener { captureVideo() }
        binding.getMusic.setOnClickListener{

            // For Data from global model
            if(myModel!=null)
            {
                if(binding.emotionText.text.toString() != "NO FACES FOUND")
                {

                }
                // Data from model
                val oldEmotion = myModel.getEmotion()
                println("Old Emotion: $oldEmotion")

                val newEmotion = binding.emotionText.text.toString()
                println("New Emotion: $newEmotion")
                myModel.setEmotion(newEmotion)

                navController.navigate(R.id.action_firstBlankFragment_to_secondBlankFragment)

            }
            //getMusic()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        // High-accuracy landmark detection and face classification
        val highAccuracyOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()

        // Real time contour detection
        val realTimeOpts = FaceDetectorOptions.Builder()
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .build()

    }

    private class LuminosityAnalyzer(private val listener: LumaListener) : ImageAnalysis.Analyzer {

        private fun ByteBuffer.toByteArray(): ByteArray{
            rewind()
            val data = ByteArray(remaining())
            get(data)
            return data
        }

        override fun analyze(image: ImageProxy) {
            val buffer = image.planes[0].buffer
            val data = buffer.toByteArray()
            val pixels = data.map { it.toInt() and 0xFF }
            val luma = pixels.average()

            listener(luma)

            image.close()
        }
    }

    private class MyImageAnalyzer(private val listener: FaceListener) : ImageAnalysis.Analyzer
    {
        @SuppressLint("UnsafeOptInUsageError")

        override fun analyze(imageProxy: ImageProxy) {

            //Log.d(TAG, "analyze: Test")
            
            val mediaImage = imageProxy.image
            if (mediaImage != null)
            {
                //println("Image from camera != null")

                val image = InputImage.fromMediaImage(
                    mediaImage, imageProxy.imageInfo.rotationDegrees)
                // Shows that image is rotated 270 degrees
                //println("Rotation ${imageProxy.imageInfo.rotationDegrees}")

                var faceData = FloatArray(6)

                // High-accuracy landmark detection and face classification
                val highAccuracyOpts = FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                    .build()

                // Real-time contour detection
                val realTimeOpts = FaceDetectorOptions.Builder()
                    .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                    .build()

                //val detector = FaceDetection.getClient(options)
                // Or, to use the default option:
                val detector = FaceDetection.getClient(highAccuracyOpts);


                // Pass image to an ML Kit Vision API
                val result = detector.process(image)
                    .addOnSuccessListener { faces ->
                        // Task completed successfully
                        // ...
                        //println("From analyzer Face Detected")

                        faceData[5] = faces.size.toFloat()

                        for (face in faces)
                        {
                            println("FACE")
                            // What is in FaceData?
                                // [0] = smileProb
                                // [1] = eyes open probability
                                // [2] = sad
                                // [3] =
                                // [4] = angry yes/no

                            val bounds = face.boundingBox


                            //val rotY = face.headEulerAngleY // Head is rotated to the right rotY degrees
                            //faceData[0] = rotY
                            //val rotZ = face.headEulerAngleZ // Head is tilted sideways rotZ degrees
                            //faceData[1] = rotZ

                            // If landmark detection was enabled (mouth, ears, eyes, cheeks, and
                            // nose available
                            val leftEar = face.getLandmark(FaceLandmark.LEFT_EAR)
                            leftEar?.let {
                                val leftEarPos = leftEar.position
                            }
                            //val leftEyeContour = face.getContour(FaceContour.LEFT_EYE)?.points
                            //val upperLipBottomContour = face.getContour(FaceContour.UPPER_LIP_BOTTOM)?.points



                            // DETECT HAPPINESS

                            // If classification was enabled:
                            if (face.smilingProbability != null) {
                                val smileProb = face.smilingProbability

                                if (smileProb != null) {
                                    faceData[0] = smileProb
                                }
                            }

                            // If contour detection was enabled:

                            // DETECT ANGRER
                            val leftEyeTopContour = face.getContour(FaceContour.LEFT_EYEBROW_TOP)?.points
                            val leftEyeBottomContour = face.getContour(FaceContour.LEFT_EYEBROW_BOTTOM)?.points

                            val eyebrowTop0 = leftEyeTopContour?.get(0)?.y
                            val eyebrowTop1 = leftEyeTopContour?.get(1)?.y
                            val eyebrowTop2 = leftEyeTopContour?.get(2)?.y
                            val eyebrowTop3 = leftEyeTopContour?.get(3)?.y
                            val eyebrowTop4 = leftEyeTopContour?.get(4)?.y

                            val eyebrowBot0 = leftEyeBottomContour?.get(0)?.y
                            val eyebrowBot1 = leftEyeBottomContour?.get(1)?.y
                            val eyebrowBot2 = leftEyeBottomContour?.get(2)?.y
                            val eyebrowBot3 = leftEyeBottomContour?.get(3)?.y
                            val eyebrowBot4 = leftEyeBottomContour?.get(4)?.y

                            //println("TOP: $eyebrowTop0,$eyebrowTop1,$eyebrowTop2,$eyebrowTop3,$eyebrowTop4")
                            //println("BOT: $eyebrowBot0,$eyebrowBot1,$eyebrowBot2,$eyebrowBot3,$eyebrowBot4")
                            // TOP: 237.0,229.0,224.0,226.0,230.0
                            // BOT: 244.0,237.0,234.0,236.0,244.0

                            if (eyebrowTop3 != null) {
                                if( eyebrowTop3 > eyebrowTop1!!) {
                                    //println("FROWN")
                                    faceData[4] = 1.0F //println(eyebrowLeft)
                                    //println(eyebrowRight)
                                }
                            }

                            // DETECT SHOCK
                            var leftProb = 0.0f
                            var rightProb = 0.0f

                            if(face.leftEyeOpenProbability != null ) {
                                val leftEyeOpenProb = face.leftEyeOpenProbability

                                if (leftEyeOpenProb != null)
                                {
                                    leftProb = leftEyeOpenProb
                                }
                            }

                            if (face.rightEyeOpenProbability != null) {
                                val rightEyeOpenProb = face.rightEyeOpenProbability
                                //println(rightEyeOpenProb) // test to print some data
                                //Log.d(TAG, "analyze: rightEyeOpenProb: $rightEyeOpenProb")
                                if(rightEyeOpenProb != null)
                                {
                                    rightProb = rightEyeOpenProb
                                }
                            }
                            faceData[1] = (leftProb + rightProb)/2
                            //println("left: $leftProb, right: $rightProb")


                            // DETECT SADNESS

                            // contours, upper lip top, upper lip bot | lower lip top, lower lip bot
                            // Find out

                            val upperLipTopContour = face.getContour(FaceContour.UPPER_LIP_TOP)?.points //L-R mid=5
                            val upperLipBotContour = face.getContour(FaceContour.UPPER_LIP_BOTTOM)?.points // L-R mid = 4
                            val lowerLipTopContour = face.getContour(FaceContour.LOWER_LIP_TOP)?.points // R-L mid = 4
                            val lowerLipBotContour = face.getContour(FaceContour.LOWER_LIP_BOTTOM)?.points // R-L mid = 4

                            if (upperLipTopContour != null) {
                                if (lowerLipTopContour != null) {
                                    if(upperLipTopContour.get(0).y > lowerLipTopContour.get(3).y) {
                                        faceData[2] = 1.0f

                                        //println("upper left: "+ upperLipTopContour.get(0).y)
                                        //println("lower mid: "+ lowerLipTopContour.get(3).y)
                                    }
                                }
                            }


                            // If face tracking was enabled:
                            if (face.trackingId != null) {
                                val id = face.trackingId

                            }
                        }
                    }
                    .addOnCompleteListener {
                            //results -> image.
                            imageProxy.close()
                            mediaImage.close()
                            listener (faceData)
                    }
                    .addOnFailureListener { e ->
                        // Task failed with an exception
                        // ...
                        println("Failing Analyzer Failing Failing Failing Failing Failing")
                        val error = e.message
                        println(error)

                    }
            }
        }
    }

    private  fun getMusic()
    {

    }

    private fun takePhoto()
    {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.UK)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        // Create output options object whihc contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(requireContext().contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues)
            .build()

        // Set up image capture listener, which is triggered after photo
        // has been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback  {
                override fun onError(exc: ImageCaptureException)  {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun
                        onImageSaved(output: ImageCapture.OutputFileResults)
                {
                            //val savedUri = Uri.fromFile(photoFile)
                            val msg = "Photo capture succeeded: ${output.savedUri}"
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG,msg)


                    /*
                     // trying to get image to analyze once image is captures
                    val imageAnalyzer = ImageAnalysis.Builder().build()
                        .also {
                            it.setAnalyzer(cameraExecutor, MyImageAnalyzer{
                                println("test - from single picture taken")



                            })
                        }
                     */
                    //imageAnalyzer
                        }
            }
        )

    }

    // Implements VideoCapture use case, including start and stop capturing.
    private fun captureVideo()
    {
        val videoCapture = this.videoCapture ?: return

        binding.videoCaptureButton.isEnabled = false

        val curRecording = recording
        if(curRecording != null){
            // Stop the current recording session
            curRecording.stop()
            recording = null
            return
        }

        // Create and start a new recording session
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.UK)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P){
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(requireContext().contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()
        recording = videoCapture.output
            .prepareRecording(requireContext(), mediaStoreOutputOptions)
            .apply{
                if(PermissionChecker.checkSelfPermission(requireContext(),
                    Manifest.permission.RECORD_AUDIO) ==
                        PermissionChecker.PERMISSION_GRANTED)
                {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(requireContext())) { recordEvent ->
                when(recordEvent) {
                    is VideoRecordEvent.Start ->{
                        binding.videoCaptureButton.apply {
                            text = getString(R.string.stop_capture)
                            isEnabled = true
                        }
                    }
                    is VideoRecordEvent.Finalize -> {
                        if(!recordEvent.hasError()){
                            val msg = "Video capture succeeded: "+
                                    "${recordEvent.outputResults.outputUri}"
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT)
                                .show()
                            Log.d(TAG,msg)
                        } else{
                            recording?.close()
                            recording = null
                            Log.e(TAG, "Video capture ends with error: "+
                                            "${recordEvent.error}")
                        }
                        binding.videoCaptureButton.apply{
                            text = getString(R.string.start_capture)
                            isEnabled = true
                        }
                    }
                }
            }
    }

    private fun startCamera()
    {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())


        cameraProviderFuture.addListener({
            // USed to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST,
                FallbackStrategy.higherQualityOrLowerThan(Quality.SD)))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            imageCapture = ImageCapture.Builder()
                .build()

            // Select back camera as a default
            //val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            /*
            val imageAnalyzer = ImageAnalysis.Builder().build()
                .also {
                    it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
                        Log.d(TAG,"Average luminosity: $luma")
                    })
                }
            */

             // trying to get image to analyze once image is captures
            val imageAnalyzer = ImageAnalysis.Builder().build()
                .also {
                    it.setAnalyzer(cameraExecutor, MyImageAnalyzer{ face ->

                        val face0 = face[0] // Smile Prob
                        val face1 = face[1] // Left eye open prob
                        val face2 = face[2] // Right eye open prob
                        val face3 = face[3] //
                        val face4 = face[4] //

                        val facesAmount = face[5]
                        //println("test0 - from continuous image capture")
                        //println("test - faces detected = $facesAmount")

                        //println("test0 - Y tilt = $face0")
                        //println("test1 - Z tilt = $face1")
                        //println("test2 - Smile Probability = $face2")
                        //println("test3 - Right eye open prob = $face3")
                        //println("test4 - Left eye open prob = $face4")
                        if(facesAmount < 1)
                        {
                            binding.emotionText.text = "NO FACES FOUND"
                        }
                        else if(face0 > 0.9)
                        {
                            binding.emotionText.text = "HAPPY"
                        }
                        else if(face4 == 1.0f)
                        {
                            binding.emotionText.text = "ANGRY"
                        }
                        else if (face2 == 1.0f)
                        {
                            binding.emotionText.text = "SAD"
                        }
                        else if(face1 > 0.994)
                        {
                            binding.emotionText.text = "SURPRISED" // surprised
                        }
                        else
                        {
                            binding.emotionText.text = "NEUTRAL"
                        }

                    })
                }
            try{
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector,preview, imageCapture, imageAnalyzer)
            } catch (exc: Exception){
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))


    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        if(requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()){
                startCamera()
            }else{
                Toast.makeText(requireContext(),
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                //finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}










