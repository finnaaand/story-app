package com.example.mystory.view

import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.transition.TransitionInflater
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.example.mystory.R
import com.example.mystory.api.ApiConfig
import com.example.mystory.data.session.SessionManager
import com.example.mystory.data.story.AddStoryResponse
import com.example.mystory.model.StoryRepository
import com.example.mystory.model.StoryViewModel
import com.example.mystory.model.ViewModelFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddStoryActivity : AppCompatActivity() {

    private lateinit var descriptionEditText: EditText
    private lateinit var photoImageView: ImageView
    private lateinit var addButton: Button
    private lateinit var cameraButton: Button
    private lateinit var galleryButton: Button
    private var photoUri: Uri? = null

    private lateinit var getContent: ActivityResultLauncher<Intent>
    private lateinit var sessionManager: SessionManager
    private val apiService = ApiConfig.apiService
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var backButton: ImageView
    private lateinit var uploadProgressBar: ProgressBar
    private lateinit var imageRequiredText: TextView

    private lateinit var storyViewModel: StoryViewModel

    companion object {
        private const val STORAGE_PERMISSION_CODE = 2001
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_story)

        sessionManager = SessionManager(this)
        checkPermissions()

        val factory = ViewModelFactory(StoryRepository(apiService), sessionManager)
        storyViewModel = ViewModelProvider(this, factory)[StoryViewModel::class.java]

        initializeViews()
        setupActivityResults()
        setupClickListeners()
        setupTransitions()

    }

    private fun initializeViews() {
        descriptionEditText = findViewById(R.id.ed_add_description)
        photoImageView = findViewById(R.id.iv_photo)
        cameraButton = findViewById(R.id.btn_camera)
        galleryButton = findViewById(R.id.btn_gallery)
        addButton = findViewById(R.id.button_add)
        backButton = findViewById(R.id.btn_back)
        uploadProgressBar = findViewById(R.id.uploadProgressBar)
        imageRequiredText = findViewById(R.id.tv_image_required)


        imageRequiredText.visibility = View.VISIBLE
        photoImageView.setBackgroundResource(R.drawable.ic_placeholder)
        descriptionEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateInput()
            }
        })
    }

    private fun validateInput() {
        try {
            val hasImage = photoUri != null
            val hasDescription = descriptionEditText.text.toString().isNotBlank()

            Log.d("AddStoryActivity", "Button State Check - Image: $hasImage, Description: $hasDescription, URI: $photoUri")

            addButton.post {
                addButton.isEnabled = hasImage && hasDescription
                Log.d("AddStoryActivity", "Button enabled: ${addButton.isEnabled}")
            }
        } catch (e: Exception) {
            Log.e("AddStoryActivity", "Error checking button state: ${e.message}")
        }
    }

    private fun setupActivityResults() {
        getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            try {
                if (result.resultCode == RESULT_OK) {
                    result.data?.data?.let { uri ->
                        photoUri = uri
                        try {
                            photoImageView.setImageURI(null)
                            photoImageView.setImageURI(uri)
                            imageRequiredText.visibility = View.GONE
                            validateInput()

                            Log.d("AddStoryActivity", "Image loaded successfully: $uri")
                        } catch (e: Exception) {
                            Log.e("AddStoryActivity", "Error setting image: ${e.message}")
                            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AddStoryActivity", "Error in activity result: ${e.message}")
                Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show()
            }
        }

        takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            try {
                if (success) {
                    photoUri?.let { uri ->
                        try {
                            photoImageView.setImageURI(null)
                            photoImageView.setImageURI(uri)
                            imageRequiredText.visibility = View.GONE
                            validateInput()

                            Log.d("AddStoryActivity", "Camera image loaded successfully: $uri")
                        } catch (e: Exception) {
                            Log.e("AddStoryActivity", "Error setting camera image: ${e.message}")
                            Toast.makeText(this, "Error loading camera image", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    photoImageView.setImageURI(null)
                    photoUri = null
                }
            } catch (e: Exception) {
                Log.e("AddStoryActivity", "Error in camera result: ${e.message}")
                Toast.makeText(this, "Error processing camera image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupClickListeners() {
        cameraButton.setOnClickListener { openCamera() }
        galleryButton.setOnClickListener { openGallery() }
        addButton.setOnClickListener {
            if (photoUri != null && descriptionEditText.text.toString().isNotBlank()) {
                animateButtonColor()
                uploadStory()
            } else {
                if (photoUri == null) {
                    Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show()
                }
                if (descriptionEditText.text.toString().isBlank()) {
                    Toast.makeText(this, "Please add a description", Toast.LENGTH_SHORT).show()
                }
            }
        }

        backButton.setOnClickListener {
            supportFinishAfterTransition()
            onBackPressedDispatcher.onBackPressed()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                supportFinishAfterTransition()
                finish()
            }
        })
    }

    private fun setupTransitions() {
        window.sharedElementEnterTransition = TransitionInflater.from(this)
            .inflateTransition(android.R.transition.move)
        window.sharedElementReturnTransition = TransitionInflater.from(this)
            .inflateTransition(android.R.transition.move)
    }

    private fun animateButtonColor() {
        val colorFrom = Color.parseColor("#415f91")
        val colorTo = Color.parseColor("#aac7ff")

        val colorAnimation = ObjectAnimator.ofArgb(addButton, "backgroundColor", colorFrom, colorTo)
        colorAnimation.duration = 250
        colorAnimation.setEvaluator(ArgbEvaluator())
        colorAnimation.start()
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                STORAGE_PERMISSION_CODE)
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        getContent.launch(intent)
    }

    private fun openCamera() {
        val photoFile = createImageFile()
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            photoFile
        )
        photoUri = uri
        takePictureLauncher.launch(uri)
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_msys", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    private fun uploadStory() {
        uploadProgressBar.isVisible = true
        val description = descriptionEditText.text.toString()

        photoUri?.let { uri ->
            if (sessionManager.isLoggedIn()) {
                val file = getFileFromUri(uri) ?: run {
                    uploadProgressBar.isVisible = false
                    return
                }

                val requestFile = file.asRequestBody("image/*".toMediaType())
                val photoPart = MultipartBody.Part.createFormData("photo", file.name, requestFile)
                val descriptionPart = description.toRequestBody("text/plain".toMediaType())

                val token = "Bearer ${sessionManager.getToken()}"

                val call = apiService.addStory(token, descriptionPart, photoPart)

                call.enqueue(object : Callback<AddStoryResponse> {
                    override fun onResponse(call: Call<AddStoryResponse>, response: Response<AddStoryResponse>) {
                        uploadProgressBar.isVisible = false
                        if (response.isSuccessful) {
                            Toast.makeText(this@AddStoryActivity, "Story uploaded successfully", Toast.LENGTH_SHORT).show()
                            setResult(Activity.RESULT_OK)
                            finish()
                        } else {
                            Toast.makeText(this@AddStoryActivity, "Upload failed: ${response.message()}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<AddStoryResponse>, t: Throwable) {
                        uploadProgressBar.isVisible = false
                        Toast.makeText(this@AddStoryActivity, "Upload failed: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            } else {
                Toast.makeText(this, "You need to log in to upload a story", Toast.LENGTH_SHORT).show()
                uploadProgressBar.isVisible = false
                finish()
            }
        }
    }

    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val file = File(cacheDir, "temp_image")
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
