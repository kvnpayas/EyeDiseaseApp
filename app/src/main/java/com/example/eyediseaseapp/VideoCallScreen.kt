package com.example.eyediseaseapp // Adjust package name

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.view.Gravity
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.* // Material 3 components
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.eyediseaseapp.util.CallNotification
import com.example.eyediseaseapp.util.MessageRepoUtils
import com.example.eyediseaseapp.util.UserUtils
import com.google.firebase.auth.FirebaseAuth // Import FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.video.VideoCanvas
import io.agora.rtc2.video.VideoEncoderConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch // Import launch
import io.agora.rtc2.ChannelMediaOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await

// --- Agora SDK Imports (Add these to your build.gradle) ---
// implementation("io.agora.rtc:full-sdk:4.3.0") // Or the latest version
// implementation("io.agora.rtm:rtm-sdk:2.1.0") // Or the latest version
// You might need to adjust versions and specific modules based on Agora documentation
// import io.agora.rtc.RtcEngine
// import io.agora.rtc.IRtcEngineEventHandler
// import io.agora.rtc.video.VideoCanvas
// import io.agora.rtm.RtmClient
// import io.agora.rtm.RtmClientListener
// import io.agora.rtm.RtmMessage
// import io.agora.rtm.ResultCallback
// import io.agora.rtm.ErrorInfo
// --- End Agora SDK Imports ---

// --- Call State Enum (Can be shared or defined here) ---
// enum class CallState { IDLE, OUTGOING, INCOMING, IN_CALL }


@Composable
fun VideoCallScreen(
    navController: NavController,
    conversationId: String, // The ID of the conversation (Patient's UID)
    modifier: Modifier = Modifier // Modifier for layout
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid // Get the current user's UID

    // --- Agora SDK: RtcEngine and RtmClient instances (Manage lifecycle) ---
    // Use remember with a key (like context or currentUserId) if you need to
    // re-initialize Agora when the key changes. If Agora should be a singleton
    // managed elsewhere (e.g., ViewModel or Application class), remove these.
    // var mRtcEngine by remember { mutableStateOf<RtcEngine?>(null) }
    // var mRtmClient by remember { mutableStateOf<RtmClient?>(null) }

    // --- State for Video Call ---
    var callState by remember { mutableStateOf(CallState.IDLE) } // Manage call state within this screen
    var remoteUserIdInCall by remember { mutableStateOf<Int?>(null) } // Agora uses Int UIDs for users in channel

    var isLocalVideoEnabled by remember { mutableStateOf(true) }
    var isRemoteVideoActive by remember { mutableStateOf(false) }

    // --- Agora Video Views ---
    // Manage these views' lifecycle and state
    var localSurfaceView by remember { mutableStateOf<SurfaceView?>(null) }
    var remoteSurfaceView by remember { mutableStateOf<SurfaceView?>(null) }

    val userRepository = remember { UserUtils() }

    val messageRepository = remember { MessageRepoUtils() }
    val coroutineScope = rememberCoroutineScope()

    var channelName by remember { mutableStateOf<String?>(null) }
    var rtcToken by remember { mutableStateOf<String?>(null) }
    var isLoadingCallDetails by remember { mutableStateOf(true) }
    var callDetailsError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(conversationId) { // Use conversationId (patientId) as the key
        Log.d("VideoCallScreen", "LaunchedEffect: Fetching call details for patient ID: $conversationId from call_notifications.")
        isLoadingCallDetails = true
        callDetailsError = null
        try {
            // Fetch the call notification document for this patient
            val notificationDocRef = FirebaseFirestore.getInstance().collection("call_notifications").document(conversationId)
            val document = notificationDocRef.get().await()

            if (document.exists()) {
                // Assuming CallNotification data class exists and has channelName and rtcToken fields
                val notification = document.toObject(CallNotification::class.java)
                if (notification != null) {
                    channelName = notification.channelName
                    rtcToken = notification.rtcToken
                    Log.d("VideoCallScreen", "Fetched channelName: $channelName, rtcToken: ${rtcToken?.take(5)}... from call_notifications.")
                } else {
                    callDetailsError = "Failed to parse call notification data."
                    Log.e("VideoCallScreen", "Failed to parse call notification document for patient ID: $conversationId")
                }
            } else {
                callDetailsError = "Call details document not found in call_notifications."
                Log.e("VideoCallScreen", "Call notification document not found for patient ID: $conversationId")
            }
        } catch (e: Exception) {
            callDetailsError = e.message ?: "Failed to fetch call details."
            Log.e("VideoCallScreen", "Error fetching call details for patient ID $conversationId: ${e.message}", e)
        } finally {
            isLoadingCallDetails = false
        }
    }

    // --- Permission Launcher for Camera and Mic ---
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
            val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
            if (cameraGranted && audioGranted) {
                Log.d("VideoCallScreen", "Camera and Audio permissions granted.")
                // Permissions granted, proceed with initiating the call ONLY if details are loaded
                if (!isLoadingCallDetails && channelName != null && rtcToken != null) {
                    initiateAgoraCall(context, currentUserId, conversationId,
                        onLocalViewCreated = { view ->
                            localSurfaceView = view
                        }, // Pass lambda to get local view
                        onRemoteViewCreated = { view ->
                            remoteSurfaceView = view
                            isRemoteVideoActive = true
                        }, // Pass lambda to get remote view
                        onRemoteViewRemoved = {
                            remoteSurfaceView = null
                            isRemoteVideoActive = false
                        }, // Pass lambda to remove remote view
                        { newState, remoteUid ->
                            // Update the composable's state based on the result of initiateAgoraCall
                            callState = newState
                            remoteUserIdInCall = remoteUid
                        },
                        coroutineScope = coroutineScope
                    )
                } else {
                    Log.w("VideoCallScreen", "Permissions granted, but call details not loaded or errored. Cannot initiate call.")
                    Toast.makeText(context, "Failed to get call details. Cannot start call.", Toast.LENGTH_LONG).show()
                    // Pass conversationId (patientId), currentUserId, and userRepository to endVideoCall for navigation and cleanup
                    endVideoCall(navController, conversationId, messageRepository, coroutineScope) // Simplified call
                }

            } else {
                Log.w("VideoCallScreen", "Camera or Audio permissions denied.")
                // Permissions denied, handle accordingly (e.g., show a message)
                Toast.makeText(
                    context,
                    "Camera and microphone permissions are required for video calls.",
                    Toast.LENGTH_LONG
                ).show()
                // Call the function defined outside the composable
                // Pass conversationId (patientId), currentUserId, and userRepository to endVideoCall for navigation and cleanup
                endVideoCall(navController, conversationId, messageRepository, coroutineScope) // Simplified call // End call attempt if permissions are denied
            }
        }
    )


    // --- Function to check and request permissions ---
    fun checkAndRequestPermissions() {
        val cameraPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        val audioPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (cameraPermission && audioPermission) {
            Log.d("VideoCallScreen", "Camera and Audio permissions already granted.")
            // Permissions already granted, proceed with call initiation ONLY if details are loaded
            if (!isLoadingCallDetails && channelName != null && rtcToken != null) {
                initiateAgoraCall(context, currentUserId, conversationId,
                    onLocalViewCreated = { view ->
                        localSurfaceView = view
                    }, // Pass lambda to get local view
                    onRemoteViewCreated = { view ->
                        remoteSurfaceView = view
                        isRemoteVideoActive = true
                    }, // Pass lambda to get remote view
                    onRemoteViewRemoved = {
                        remoteSurfaceView = null
                        isRemoteVideoActive = false
                    }, // Pass lambda to remove remote view
                    { newState, remoteUid ->
                        // Update the composable's state based on the result of initiateAgoraCall
                        callState = newState
                        remoteUserIdInCall = remoteUid
                    },
                    coroutineScope = coroutineScope
                )
            } else {
                Log.w("VideoCallScreen", "Permissions granted, but call details not loaded or errored. Cannot initiate call.")
                Toast.makeText(context, "Failed to get call details. Cannot start call.", Toast.LENGTH_LONG).show()
                // Pass conversationId (patientId), currentUserId, and userRepository to endVideoCall for navigation and cleanup
                endVideoCall(navController, conversationId, messageRepository, coroutineScope) // Simplified call
            }
        } else {
            Log.d("VideoCallScreen", "Requesting Camera and Audio permissions.")
            // Request permissions
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )
        }
    }


    // --- Trigger permission check and call initiation when screen enters composition ---
    // This assumes the screen is navigated to when a call is intended to start.
    LaunchedEffect(isLoadingCallDetails, channelName, rtcToken) { // Keys are loading state and fetched details
        Log.d("VideoCallScreen", "LaunchedEffect (Call Initiation): isLoadingCallDetails: $isLoadingCallDetails, channelName: $channelName, rtcToken: ${rtcToken?.take(5)}...")
        if (!isLoadingCallDetails && channelName != null && rtcToken != null) {
            Log.d("VideoCallScreen", "Call details loaded, checking permissions to initiate Agora call.")
            callState = CallState.OUTGOING // Set state to outgoing while connecting
            checkAndRequestPermissions()
        } else if (!isLoadingCallDetails && (channelName == null || rtcToken == null)) {
            // Call details loaded, but missing channel or token
            Log.e("VideoCallScreen", "Call details loaded, but channel name or token is null. Cannot initiate call.")
            Toast.makeText(context, callDetailsError ?: "Failed to get call details.", Toast.LENGTH_LONG).show()
            callState = CallState.IDLE // Set state to idle on failure
            // Pass conversationId (patientId), currentUserId, and userRepository to endVideoCall for navigation and cleanup
            endVideoCall(navController, conversationId, messageRepository, coroutineScope) // Simplified call
        } else if (callDetailsError != null) {
            // Call details fetching failed
            Log.e("VideoCallScreen", "Call details fetching failed. Cannot initiate call.")
            Toast.makeText(context, callDetailsError ?: "Failed to get call details.", Toast.LENGTH_LONG).show()
            callState = CallState.IDLE // Set state to idle on failure
            // Pass conversationId (patientId), currentUserId, and userRepository to endVideoCall for navigation and cleanup
            endVideoCall(navController, conversationId, messageRepository, coroutineScope) // Simplified call
        }
        // If isLoadingCallDetails is true, this LaunchedEffect will wait.
    }

    LaunchedEffect(conversationId) {
        Log.d("VideoCallScreen", "LaunchedEffect (Status Listener): Starting listener for patient ID: $conversationId")
        // Assuming MessageRepoUtils has a getCallNotificationForPatient function that returns a Flow
        messageRepository.getCallNotificationForPatient(conversationId).collect { notification ->
            Log.d("VideoCallScreen", "Status Listener: Received notification update: $notification")
            // If the notification status changes to "rejected", "ended", or "missed", end the call
            if (notification?.status == "rejected" || notification?.status == "ended" || notification?.status == "missed") {
                Log.d("VideoCallScreen", "Status Listener: Call status changed to ${notification.status}. Ending call.")
                // Use the existing endVideoCall function to clean up Agora and navigate
                // We don't need to update the status again here, as it was updated by the other party
                // Pass the necessary dependencies to endVideoCall
                endVideoCall(navController, conversationId, messageRepository, coroutineScope) // Simplified call
            } else if (notification == null) {
                // If the document is deleted while on the call screen (e.g., doctor deletes it)
                Log.d("VideoCallScreen", "Status Listener: Call notification document deleted. Ending call.")
                // Pass the necessary dependencies to endVideoCall
                endVideoCall(navController, conversationId, messageRepository, coroutineScope) // Simplified call
            }
            // If status is "calling" or "accepted", the call continues, no action needed here.
        }
    }


    // --- Cleanup when the composable leaves the composition ---
    DisposableEffect(Unit) {
        onDispose {
            Log.d("VideoCallScreen", "VideoCallScreen leaving composition. Ending call and cleaning up Agora and notification.")
            // Ensure call is ended and Agora resources are released
            // Call the function defined outside the composable
            // We update the status to "ended" so the other party knows.
            // The status listener above might also trigger if the other party ended first.
            // It's safe to call update here; it will just overwrite if the status is already ended/rejected/missed.
            coroutineScope.launch {
                try {
                    // Check current status before updating to avoid overwriting a terminated state
                    val notification = messageRepository.getCallNotificationForPatient(conversationId).firstOrNull() // Get current state
                    if (notification?.status == "calling" || notification?.status == "accepted") {
                        messageRepository.updateCallNotificationStatus(conversationId, "ended")
                        Log.d("VideoCallScreen", "DisposableEffect: Updated call notification status to 'ended' for patient $conversationId.")
                    } else {
                        Log.d("VideoCallScreen", "DisposableEffect: Call notification status is already ${notification?.status ?: "null"}. No status update needed.")
                    }
                } catch (e: Exception) {
                    Log.e("VideoCallScreen", "DisposableEffect: Failed to update call notification status to 'ended' for patient $conversationId: ${e.message}", e)
                } finally {
                    // Ensure Agora engine is destroyed regardless of status update success
                    val engine = mRtcEngine
                    if (engine != null) {
                        engine.stopPreview()
                        engine.leaveChannel()
                        engine.enableLocalVideo(false)
                        engine.enableLocalAudio(false) // Disable local audio as well
                        RtcEngine.destroy()
                        mRtcEngine = null
                        Log.d("AgoraRTC", "DisposableEffect: RtcEngine destroyed.")
                    } else {
                        Log.d("AgoraRTC", "DisposableEffect: RtcEngine instance is null.")
                    }
                }
            }
        }
    }


    // --- Layout for Video Call Screen ---
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 16.dp) // Add top padding here
    ) {
        Column( // Use a Column to stack local and remote views vertically
            modifier = Modifier.fillMaxSize()
        ) {
            // --- Remote Video View (Top Half) ---
            AndroidView(
                factory = { context ->
                    FrameLayout(context).apply { }
                },
                update = { frameLayout ->
                    Log.d("VideoCallScreen", "Remote AndroidView update triggered. remoteSurfaceView is: ${remoteSurfaceView}")
                    frameLayout.removeAllViews()
                    if (isRemoteVideoActive && remoteSurfaceView != null) {
                        remoteSurfaceView?.let { view ->
                            (view.parent as? ViewGroup)?.removeView(view) // Remove from old parent if any
                            frameLayout.addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)) // Add with layout params
                            Log.d("VideoCallScreen", "Added remoteSurfaceView to Remote FrameLayout because video is active.")
                        } ?: Log.d("VideoCallScreen", "remoteSurfaceView is null unexpectedly when trying to add.")
                    } else {
                        // If remote video is NOT active, show "No camera" text
                        Log.d("VideoCallScreen", "Remote video is not active or remoteSurfaceView is null, showing 'No camera' text.")
                        val textView = TextView(context).apply {
                            text = "No camera"
                            setTextColor(android.graphics.Color.WHITE) // White text color
                            textSize = 18f // Text size
                            gravity = Gravity.CENTER // Center the text within the TextView's bounds
                        }
                        frameLayout.addView(textView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply {
                            gravity = Gravity.CENTER // Center the TextView within the FrameLayout
                        })
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Make it take 50% of the available vertical space
                    .background(Color.Black) // Placeholder background
            )

            // --- Local Video View (Bottom Half) ---
            AndroidView(
                factory = { context ->
                    FrameLayout(context).apply { }
                },
                update = { frameLayout ->
                    Log.d("VideoCallScreen", "Local AndroidView update triggered. localSurfaceView is: ${localSurfaceView}")
                    frameLayout.removeAllViews()
                    if (isLocalVideoEnabled) {
                        localSurfaceView?.let { view ->
                            (view.parent as? ViewGroup)?.removeView(view)
                            frameLayout.addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)) // Add with layout params
                            Log.d("VideoCallScreen", "Added localSurfaceView to Local FrameLayout because video is enabled.")
                        } ?: Log.d("VideoCallScreen", "localSurfaceView is null but video is enabled, cannot add.")
                    } else {
                        // When video is disabled, the FrameLayout will be empty, showing its background
                        Log.d("VideoCallScreen", "Local video is disabled, removing views from local container.")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Make it take 50% of the available vertical space
                    .background(Color.DarkGray) // Placeholder background
            )
        }


        // --- Call Status Text (Overlayed on top) ---
        val statusText = when (callState) {
            CallState.IDLE -> "Call Ended"
            CallState.OUTGOING -> "Calling..."
            CallState.IN_CALL -> if (remoteUserIdInCall != null) "In Call" else "Waiting for participant..."
            CallState.INCOMING -> "Incoming Call..."
        }
        Text(
            text = statusText,
            color = Color.White,
            fontSize = 18.sp,
            modifier = Modifier
                .align(Alignment.TopCenter) // Position in top-center
                .padding(top = 8.dp) // Add some padding below the top edge
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter) // Position at the bottom-center
                .padding(bottom = 32.dp) // Add padding above the bottom edge
                .fillMaxWidth()
                .padding(horizontal = 16.dp), // Add horizontal padding
            horizontalArrangement = Arrangement.SpaceEvenly, // Distribute buttons evenly
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- Toggle Video Button ---
            Button(
                onClick = {
                    Log.d("VideoCallScreen", "Toggle Video Button Clicked. Current state: $isLocalVideoEnabled")
                    val engine = mRtcEngine
                    if (engine != null) {
                        if (isLocalVideoEnabled) {
                            // Disable local video
                            engine.enableLocalVideo(false)
                            Log.d("AgoraRTC", "enableLocalVideo(false) called.")
                            isLocalVideoEnabled = false // Update state
                        } else {
                            // Enable local video
                            engine.enableLocalVideo(true)
                            Log.d("AgoraRTC", "enableLocalVideo(true) called.")
                            isLocalVideoEnabled = true // Update state
                        }
                    } else {
                        Log.w("VideoCallScreen", "RtcEngine is null, cannot toggle video.")
                        Toast.makeText(context, "Call not active, cannot toggle video.", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray) // Example color
            ) {
                Icon(
                    imageVector = if (isLocalVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    contentDescription = if (isLocalVideoEnabled) "Disable Video" else "Enable Video"
                )
            }

            // --- End Call Button ---
            Button(
                onClick = {
                    Log.d("VideoCallScreen", "End Call Button Clicked")
                    // This button should trigger the end call logic which also navigates back
                    // The DisposableEffect will handle the cleanup when navigation happens
                    endVideoCall(navController, conversationId, messageRepository, coroutineScope)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Icon(
                    imageVector = Icons.Default.Close, // Using Close icon for End Call
                    contentDescription = "End Video Call"
                )
                Spacer(modifier = Modifier.width(4.dp)) // Optional space
                Text("End Call")
            }

            // --- Toggle Audio Button (Placeholder for future) ---
            // You can add a similar button for audio using enableLocalAudio(true/false)
            // Button(
            //     onClick = { /* TODO: Implement audio toggle */ },
            //     colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            // ) {
            //     Icon(
            //         imageVector = Icons.Default.Mic, // Requires material-icons-core
            //         contentDescription = "Toggle Audio"
            //     )
            // }
        }
    }
}

private var mRtcEngine: RtcEngine? = null


// --- Agora SDK: Function to Initiate Video Call (Moved outside Composable) ---
// This function will handle Agora engine initialization and joining the channel
fun initiateAgoraCall(
    context: Context, // Use android.content.Context explicitly
    currentUserId: String?,
    conversationId: String,
    onLocalViewCreated: (SurfaceView) -> Unit,
    onRemoteViewCreated: (SurfaceView) -> Unit,
    onRemoteViewRemoved: () -> Unit,
    stateUpdate: (CallState, Int?) -> Unit,
    coroutineScope: CoroutineScope
) {
    if (currentUserId == null) {
        Log.w("VideoCallScreen", "Cannot initiate Agora call: User not logged in.")
        Toast.makeText(context, "User not logged in. Cannot start call.", Toast.LENGTH_SHORT).show()
        stateUpdate(CallState.IDLE, null)
        return
    }

    Log.d("VideoCallScreen", "Initiating Agora call for conversation: $conversationId")

    // --- Fetch channel details from Firestore ---
    val firestore = FirebaseFirestore.getInstance()
    val channelDocRef = firestore.collection("channel_name_id").document("static_channel_name")

    // Fetch the document asynchronously
    channelDocRef.get().addOnCompleteListener { task ->
        if (task.isSuccessful) {
            val document = task.result
            if (document != null && document.exists()) {
                val appId = document.getString("app_id")
                val channelName = document.getString("channel_name")
                val rtcToken = document.getString("token")

                if (!appId.isNullOrBlank() && !channelName.isNullOrBlank() && !rtcToken.isNullOrBlank()) {
                    Log.d("AgoraRTC", "Fetched appId: $appId, channelName: $channelName, rtcToken: ${rtcToken.take(5)}...")

                    // --- Agora SDK: Initialize RtcEngine using fetched App ID ---
                    // Initialize the RtcEngine instance if it's not already initialized
                    if (mRtcEngine == null) {
                        try {
                            Log.d("AgoraRTC", "Initializing RtcEngine with App ID: $appId")
                            mRtcEngine = RtcEngine.create(context, appId, object : IRtcEngineEventHandler() {
                                override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                                    Log.d("AgoraRTC", "Joined channel $channel with uid $uid")
                                    stateUpdate(CallState.IN_CALL, null)

                                    Log.d("AgoraRTC", "onJoinChannelSuccess: Audio and Video enabled/local enabled.")

                                    // --- Configure Video Encoder (Optional but Recommended) ---
                                    // Helps control resolution, frame rate, bitrate
//                                    val videoConfig = VideoEncoderConfiguration(
//                                        VideoEncoderConfiguration.VD_640x360, // Resolution
//                                        VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15, // Frame rate
//                                        VideoEncoderConfiguration.STANDARD_BITRATE, // Bitrate
//                                        VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE // Orientation
//                                    )
//                                    mRtcEngine?.setVideoEncoderConfiguration(videoConfig)
//                                    Log.d("AgoraRTC", "onJoinChannelSuccess: Video encoder configured.")

                                }
                                override fun onUserJoined(uid: Int, elapsed: Int) {
                                    Log.d("AgoraRTC", "Remote user joined: $uid")
                                    stateUpdate(CallState.IN_CALL, uid)
                                    setupRemoteVideo(context, mRtcEngine, uid, onRemoteViewCreated)
                                }
                                override fun onUserOffline(uid: Int, reason: Int) {
                                    Log.d("AgoraRTC", "Remote user offline: $uid, reason: $reason")
                                    stateUpdate(CallState.IN_CALL, null)
                                    onRemoteViewRemoved()
                                }
                                override fun onError(err: Int) {
                                    Log.e("AgoraRTC", "Agora Error: $err")
                                    Toast.makeText(context, "Agora Error: $err", Toast.LENGTH_SHORT).show()
                                    stateUpdate(CallState.IDLE, null)
                                }
//                                override fun onLocalVideoStateChanged(state: Int, error: Int) {
//                                    Log.d("AgoraRTC", "Local video state changed: $state, error: $error")
//                                    when(state) {
//                                        Constants.LOCAL_VIDEO_STREAM_STATE_STOPPED -> Log.d("AgoraRTC", "Local video stream stopped.")
//                                        Constants.LOCAL_VIDEO_STREAM_STATE_CAPTURING -> Log.d("AgoraRTC", "Local video stream capturing.")
//                                        Constants.LOCAL_VIDEO_STREAM_STATE_ENCODING -> Log.d("AgoraRTC", "Local video stream encoding.")
//                                        Constants.LOCAL_VIDEO_STREAM_STATE_FAILED -> Log.e("AgoraRTC", "Local video stream failed. Error: $error")
//                                    }
//                                }

                                override fun onLocalVideoStateChanged(
                                    source: Constants.VideoSourceType?,
                                    state: Int,
                                    error: Int
                                ) {
                                    Log.d("AgoraRTC", "onLocalVideoStateChanged: Source: $source, State: $state, Error: $error")
                                    when (state) {
                                        Constants.LOCAL_VIDEO_STREAM_STATE_STOPPED -> Log.d("AgoraRTC", "Local video stream stopped.")
                                        Constants.LOCAL_VIDEO_STREAM_STATE_CAPTURING -> Log.d("AgoraRTC", "Local video stream capturing.")
                                        Constants.LOCAL_VIDEO_STREAM_STATE_ENCODING -> Log.d("AgoraRTC", "Local video stream encoding.")
                                        Constants.LOCAL_VIDEO_STREAM_STATE_FAILED -> Log.e("AgoraRTC", "Local video stream failed. Error: $error")
                                    }
                                    // You can add more specific handling here based on state and error
                                    if (state == Constants.LOCAL_VIDEO_STREAM_STATE_FAILED) {
                                        // Log the specific error code
                                        Log.e("AgoraRTC", "Local video stream failed with error: $error")
                                        // Consider showing a Toast or updating UI state to reflect the failure
                                    }
                                }


                                override fun onRemoteVideoStateChanged(uid: Int, state: Int, reason: Int, elapsed: Int) {
                                    val stateString = when(state) {
                                        Constants.REMOTE_VIDEO_STATE_STOPPED -> "STOPPED"
                                        Constants.REMOTE_VIDEO_STATE_STARTING -> "STARTING"
                                        Constants.REMOTE_VIDEO_STATE_DECODING -> "DECODING"
                                        Constants.REMOTE_VIDEO_STATE_FAILED -> "FAILED"
                                        else -> "UNKNOWN($state)"
                                    }
                                    val reasonString = when(reason) {
                                        Constants.REMOTE_VIDEO_STATE_REASON_REMOTE_MUTED -> "REMOTE_MUTED"
                                        Constants.REMOTE_VIDEO_STATE_REASON_REMOTE_UNMUTED -> "REMOTE_UNMUTED"
                                        Constants.REMOTE_VIDEO_STATE_REASON_LOCAL_MUTED -> "LOCAL_MUTED"
                                        Constants.REMOTE_VIDEO_STATE_REASON_LOCAL_UNMUTED -> "LOCAL_UNMUTED"
                                        Constants.REMOTE_VIDEO_STATE_REASON_REMOTE_OFFLINE -> "REMOTE_OFFLINE"
                                        Constants.REMOTE_VIDEO_STATE_REASON_AUDIO_FALLBACK -> "AUDIO_FALLBACK"
                                        Constants.REMOTE_VIDEO_STATE_REASON_INTERNAL -> "INTERNAL" // Add internal reason
                                        Constants.REMOTE_VIDEO_STATE_REASON_NETWORK_CONGESTION -> "NETWORK_CONGESTION" // Add network congestion reason
                                        Constants.REMOTE_VIDEO_STATE_REASON_CODEC_NOT_SUPPORT -> "CODEC_NOT_SUPPORT" // Add codec not support reason
                                        else -> "UNKNOWN_REASON($reason)"
                                    }
                                    Log.d("AgoraRTC", "onRemoteVideoStateChanged: UID $uid, State: $stateString ($state), Reason: $reasonString ($reason)")
                                    // If remote video state changes to RENDERING, it means the stream is received and displayed
                                }


                            })
                            Log.d("AgoraRTC", "RtcEngine initialized with fetched App ID.")
                        } catch (e: Exception) {
                            Log.e("VideoCallScreen", "Failed to initialize Agora RtcEngine: ${e.message}", e)
                            Toast.makeText(context, "Failed to initialize video call.", Toast.LENGTH_SHORT).show()
                            stateUpdate(CallState.IDLE, null)
                            return@addOnCompleteListener // Exit if engine initialization fails
                        }
                    } else {
                        Log.d("AgoraRTC", "RtcEngine already initialized.")
                    }

                    // Agora SDK: Enable video
                    mRtcEngine?.enableVideo()
                    mRtcEngine?.enableAudio() // Ensure audio is enabled
                    Log.d("AgoraRTC", "Video and Audio enabled.")

                    // Explicitly enable local video capture and rendering
                    mRtcEngine?.enableLocalVideo(true)
                    mRtcEngine?.enableLocalAudio(true)
                    Log.d("AgoraRTC", "Local video enabled explicitly.")

                    setupLocalVideo(context, mRtcEngine, onLocalViewCreated)

                    val startPreviewResult = mRtcEngine?.startPreview()
                    Log.d("AgoraRTC", "startPreview result: $startPreviewResult (0 indicates success)")
                    coroutineScope.launch { // Launch a coroutine to perform the delay
                        delay(500)

                    val options = ChannelMediaOptions()
                    options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER // Or PUBLISHER
                    options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION // Or LIVE_BROADCASTING

                    // --- Agora SDK: Join Channel using fetched details ---
                    val uid = 0 // Using 0 for Agora to assign a UID
                    val joinChannelResult = mRtcEngine?.joinChannel(rtcToken, channelName, uid, options) // Pass options
                    Log.d("AgoraRTC", "joinChannel result: $joinChannelResult (0 indicates success)")

                    if (joinChannelResult != 0) {
                        Log.e("AgoraRTC", "Failed to join channel. Result code: $joinChannelResult")
                        Toast.makeText(context, "Failed to join call channel.", Toast.LENGTH_SHORT).show()
                        stateUpdate(CallState.IDLE, null)
                    }
                    }



                } else {
                    Log.e("AgoraRTC", "Firestore document 'static_channel_name' is missing 'app_id', 'channel_name', or 'token' fields.")
                    Toast.makeText(context, "Failed to get channel details from database (missing fields).", Toast.LENGTH_SHORT).show()
                    stateUpdate(CallState.IDLE, null)
                }
            } else {
                Log.e("AgoraRTC", "Firestore document 'static_channel_name' not found in 'channel_name_id' collection.")
                Toast.makeText(context, "Channel details document not found in database.", Toast.LENGTH_SHORT).show()
                stateUpdate(CallState.IDLE, null)
            }
        } else {
            Log.e("AgoraRTC", "Error fetching channel details from Firestore: ${task.exception?.message}")
            Toast.makeText(context, "Error fetching channel details from database.", Toast.LENGTH_SHORT).show()
            stateUpdate(CallState.IDLE, null)
        }
    }
}


// --- Agora SDK: Function to End Video Call (Moved outside Composable) ---
// This function will handle leaving the channel and cleaning up Agora resources
fun endVideoCall(
    navController: NavController,
    patientId: String, // The patientId (conversationId) for notification cleanup
    messageRepository: MessageRepoUtils, // Assuming MessageRepoUtils is your repository
    coroutineScope: CoroutineScope // Pass CoroutineScope for async operations
) {
    Log.d("VideoCallScreen", "Ending video call...")
    val engine = mRtcEngine
    if (engine != null) {
        engine.stopPreview()
        Log.d("AgoraRTC", "Preview stopped.")

        engine.leaveChannel()
        Log.d("AgoraRTC", "Left channel.")

        engine.enableLocalVideo(false)
        engine.enableLocalAudio(false) // Disable local audio as well
        Log.d("AgoraRTC", "Local video and audio disabled explicitly.")


        RtcEngine.destroy() // Use engine.destroy() on the instance
        Log.d("AgoraRTC", "RtcEngine destroyed.")
        mRtcEngine = null

        // --- Update the call notification document and navigate ---
        coroutineScope.launch {
            try {
                // Check current status before updating to avoid overwriting a terminated state
                val notification = messageRepository.getCallNotificationForPatient(patientId).firstOrNull() // Get current state
                if (notification?.status == "calling" || notification?.status == "accepted") {
                    messageRepository.updateCallNotificationStatus(patientId, "ended")
                    Log.d("VideoCallScreen", "End Call: Updated call notification status to 'ended' for patient $patientId.")
                } else {
                    Log.d("VideoCallScreen", "End Call: Call notification status is already ${notification?.status ?: "null"}. No status update needed.")
                }
            } catch (e: Exception) {
                Log.e("VideoCallScreen", "End Call: Failed to update call notification status to 'ended' for patient $patientId: ${e.message}", e)
                // Handle error (e.g., show a Toast, although call is ending anyway)
            } finally {
                // --- Navigate directly to ConversationsListScreen ---
                Log.d("VideoCallScreen", "End Call: Navigating to ConversationsListScreen.")
                navController.navigate(Screen.ConversationsList.route) {
                    // Pop up to ConversationsList to avoid multiple video call screens in stack
                    popUpTo(Screen.ConversationsList.route) { inclusive = false }
                    launchSingleTop = true // Avoid creating multiple copies
                }
            }
        }
    } else {
        Log.d("AgoraRTC", "RtcEngine instance is null, no need to leave or destroy.")
        // --- Still attempt to update/delete the call notification if engine was null ---
        coroutineScope.launch {
            try {
                // Attempt to update status even if engine is null
                messageRepository.updateCallNotificationStatus(patientId, "ended")
                Log.d("VideoCallScreen", "End Call (Engine Null): Call notification status updated to 'ended' for patient $patientId.")
            } catch (e: Exception) {
                Log.e("VideoCallScreen", "End Call (Engine Null): Failed to update call notification status for patient $patientId: ${e.message}", e)
            } finally {
                // --- Navigate directly to ConversationsListScreen (even if engine was null) ---
                Log.d("VideoCallScreen", "End Call (Engine Null): Navigating to ConversationsListScreen.")
                navController.navigate(Screen.ConversationsList.route) {
                    popUpTo(Screen.ConversationsList.route) { inclusive = false }
                    launchSingleTop = true
                }
            }
        }
    }
}

fun setupLocalVideo(
    context: android.content.Context,
    mRtcEngine: RtcEngine?,
    onLocalViewCreated: (SurfaceView) -> Unit
) {
    // This function would be called from onJoinChannelSuccess
    Log.d("AgoraRTC", "Setting up local video.")
    val surfaceView = SurfaceView(context)
    surfaceView.setZOrderMediaOverlay(true) // Make local view appear on top

    // Pass the created SurfaceView back to the Composable
    onLocalViewCreated(surfaceView)

    // Set up the local video stream
    val localVideoCanvas = VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0)
    val setupResult = mRtcEngine?.setupLocalVideo(localVideoCanvas)
    Log.d("AgoraRTC", "setupLocalVideo result: $setupResult (0 indicates success)")
}


fun setupRemoteVideo(
    context: android.content.Context,
    mRtcEngine: RtcEngine?,
    uid: Int,
    onRemoteViewCreated: (SurfaceView) -> Unit
) {
    // This function would be called from onUserJoined
    Log.d("AgoraRTC", "Setting up remote video for UID: $uid")
    val surfaceView = SurfaceView(context)
    // No need for setZOrderMediaOverlay(true) for the background remote view

    // Pass the created SurfaceView back to the Composable
    onRemoteViewCreated(surfaceView)

    // Set up the remote video stream
    val remoteVideoCanvas = VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid)
    val setupResult = mRtcEngine?.setupRemoteVideo(remoteVideoCanvas)
    Log.d("AgoraRTC", "setupRemoteVideo result for UID $uid: $setupResult (0 indicates success)")
}


// --- Agora SDK: Function to Remove Remote Video View (Moved outside Composable) ---
// This function needs access to the remoteSurfaceView state
/*
fun removeRemoteVideo(remoteSurfaceView: SurfaceView?) {
    // This function would be called from onUserOffline or when ending the call
    // It removes the remote video view from its parent
    // remoteSurfaceView?.let { (it.parent as? ViewGroup)?.removeView(it) }
    // remoteSurfaceView = null
}
*/

// --- Call State Enum (Can be shared or defined here) ---
enum class CallState {
    IDLE, // Not in a call
    OUTGOING, // Initiating a call
    INCOMING, // Receiving a call
    IN_CALL // Currently in a call
}