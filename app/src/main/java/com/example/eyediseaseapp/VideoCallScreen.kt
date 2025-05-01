package com.example.eyediseaseapp // Adjust package name

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.FrameLayout
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
import com.example.eyediseaseapp.util.MessageRepoUtils
import com.google.firebase.auth.FirebaseAuth // Import FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.video.VideoCanvas
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch // Import launch

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

    // --- Agora Video Views ---
    // Manage these views' lifecycle and state
    var localSurfaceView by remember { mutableStateOf<SurfaceView?>(null) }
    var remoteSurfaceView by remember { mutableStateOf<SurfaceView?>(null) }

    val messageRepository = remember { MessageRepoUtils() }
    val coroutineScope = rememberCoroutineScope()

    // --- Permission Launcher for Camera and Mic ---
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
            val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
            if (cameraGranted && audioGranted) {
                Log.d("VideoCallScreen", "Camera and Audio permissions granted.")
                // Permissions granted, proceed with initiating the call
                // Call the function defined outside the composable
                initiateAgoraCall(context, currentUserId, conversationId,
                    onLocalViewCreated = { view ->
                        localSurfaceView = view
                    }, // Pass lambda to get local view
                    onRemoteViewCreated = { view ->
                        remoteSurfaceView = view
                    }, // Pass lambda to get remote view
                    onRemoteViewRemoved = {
                        remoteSurfaceView = null
                    } // Pass lambda to remove remote view
                ) { newState, remoteUid ->
                    // Update the composable's state based on the result of initiateAgoraCall
                    callState = newState
                    remoteUserIdInCall = remoteUid
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
                endVideoCall(navController, conversationId, messageRepository, coroutineScope) // End call attempt if permissions are denied
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
            // Permissions already granted, proceed with call initiation
            // Call the function defined outside the composable
            initiateAgoraCall(context, currentUserId, conversationId,
                onLocalViewCreated = { view ->
                    localSurfaceView = view
                }, // Pass lambda to get local view
                onRemoteViewCreated = { view ->
                    remoteSurfaceView = view
                }, // Pass lambda to get remote view
                onRemoteViewRemoved = {
                    remoteSurfaceView = null
                } // Pass lambda to remove remote view
            ) { newState, remoteUid ->
                // Update the composable's state based on the result of initiateAgoraCall
                callState = newState
                remoteUserIdInCall = remoteUid
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
    LaunchedEffect(Unit) {
        Log.d("VideoCallScreen", "VideoCallScreen entered composition. Checking permissions.")
        // We set the state to OUTGOING *before* checking permissions, as the check
        // is part of the initiation process. The state will change to IN_CALL
        // upon successful channel join or back to IDLE/error on failure.
        callState = CallState.OUTGOING
        checkAndRequestPermissions() // Start the permission check and call initiation process
    }


    // --- Cleanup when the composable leaves the composition ---
    DisposableEffect(Unit) {
        onDispose {
            Log.d(
                "VideoCallScreen",
                "VideoCallScreen leaving composition. Ending call and cleaning up Agora."
            )
            // Ensure call is ended and Agora resources are released
            // Call the function defined outside the composable
            endVideoCall(navController, conversationId, messageRepository, coroutineScope) // End any active call and navigate back
            // Note: endVideoCall already handles cleanup and navigation back.
            // We call it here in onDispose to ensure cleanup if the screen is
            // dismissed by means other than clicking the End Call button (e.g., back button).
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
                    remoteSurfaceView?.let { view ->
                        (view.parent as? ViewGroup)?.removeView(view)
                        frameLayout.addView(view)
                        Log.d("VideoCallScreen", "Added remoteSurfaceView to Remote FrameLayout.")
                    } ?: Log.d("VideoCallScreen", "remoteSurfaceView is null, removing views from remote container.")
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
                    localSurfaceView?.let { view ->
                        (view.parent as? ViewGroup)?.removeView(view)
                        frameLayout.addView(view)
                        Log.d("VideoCallScreen", "Added localSurfaceView to Local FrameLayout.")
                    } ?: Log.d("VideoCallScreen", "localSurfaceView is null, removing views from local container.")
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

        // --- End Call Button (Overlayed at the bottom) ---
        Button(
            onClick = {
                Log.d("VideoCallScreen", "End Call Button Clicked")
                endVideoCall(navController, conversationId, messageRepository, coroutineScope)
            },
            modifier = Modifier
                .align(Alignment.BottomCenter) // Position at the bottom-center
                .padding(bottom = 32.dp), // Add padding above the bottom edge
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "End Video Call"
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("End Call")
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
    stateUpdate: (CallState, Int?) -> Unit
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

                                    setupLocalVideo(context, mRtcEngine, onLocalViewCreated)

                                    val startPreviewResult = mRtcEngine?.startPreview()
                                    Log.d("AgoraRTC", "startPreview result: $startPreviewResult (0 indicates success)")
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
                    Log.d("AgoraRTC", "Video enabled.")

                    // Explicitly enable local video capture and rendering
                    mRtcEngine?.enableLocalVideo(true)
                    Log.d("AgoraRTC", "Local video enabled explicitly.")

                    // --- Agora SDK: Join Channel using fetched details ---
                    val uid = 0 // Using 0 for Agora to assign a UID
                    val joinChannelResult = mRtcEngine?.joinChannel(rtcToken, channelName, "", uid)
                    Log.d("AgoraRTC", "joinChannel result: $joinChannelResult (0 indicates success)")

                    if (joinChannelResult != 0) {
                        Log.e("AgoraRTC", "Failed to join channel. Result code: $joinChannelResult")
                        Toast.makeText(context, "Failed to join call channel.", Toast.LENGTH_SHORT).show()
                        stateUpdate(CallState.IDLE, null)
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
    messageRepository: MessageRepoUtils,
    coroutineScope: CoroutineScope
) {
    Log.d("VideoCallScreen", "Ending video call...")
    val engine = mRtcEngine
    if (engine != null) {
        engine.stopPreview()
        Log.d("AgoraRTC", "Preview stopped.")

        engine.leaveChannel()
        Log.d("AgoraRTC", "Left channel.")

        engine.enableLocalVideo(false)
        Log.d("AgoraRTC", "Local video disabled explicitly.")

//        engine.destroy()
        Log.d("AgoraRTC", "RtcEngine destroyed.")
        mRtcEngine = null

        // --- Delete the call notification document ---
        // Use the provided coroutineScope to launch the suspend function
        coroutineScope.launch {
            try {
                messageRepository.deleteCallNotification(patientId)
                Log.d("VideoCallScreen", "Call notification deleted for patient $patientId.")
            } catch (e: Exception) {
                Log.e("VideoCallScreen", "Failed to delete call notification for patient $patientId: ${e.message}", e)
                // Handle error (e.g., show a Toast, although call is ending anyway)
            }
        }

    } else {
        Log.d("AgoraRTC", "RtcEngine instance is null, no need to leave or destroy.")
        // --- Still attempt to delete the call notification if engine was null ---
        coroutineScope.launch {
            try {
                messageRepository.deleteCallNotification(patientId)
                Log.d("VideoCallScreen", "Call notification deleted for patient $patientId (engine was null).")
            } catch (e: Exception) {
                Log.e("VideoCallScreen", "Failed to delete call notification for patient $patientId (engine was null): ${e.message}", e)
            }
        }
    }

    Log.d("VideoCallScreen", "Video call ended. Navigating back.")
    navController.popBackStack()
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