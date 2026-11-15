import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Gym Assistant',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.indigo),
        useMaterial3: true,
      ),
      home: const LandingPage(),
    );
  }
}

/// One result from the native pipeline: MediaPipe landmarks (Kotlin) plus
/// the exercise analysis computed by backend.py (Python).
class PoseResult {
  const PoseResult({
    required this.landmarkCount,
    required this.reps,
    this.stage,
    this.feedback,
    this.error,
    this.angles = const {},
  });

  final int landmarkCount;
  final int reps;
  final String? stage;
  final String? feedback;
  final String? error;
  final Map<String, double> angles;

  static const empty = PoseResult(landmarkCount: 0, reps: 0);

  factory PoseResult.fromJson(String payload) {
    final decoded = jsonDecode(payload) as Map<String, dynamic>;
    final analysis = decoded['analysis'] as Map<String, dynamic>? ?? const {};
    final angles = (analysis['angles'] as Map<String, dynamic>? ?? const {})
        .map((key, value) => MapEntry(key, (value as num).toDouble()));
    return PoseResult(
      landmarkCount: (decoded['landmarks'] as List<dynamic>? ?? const []).length,
      reps: (analysis['reps'] as num?)?.toInt() ?? 0,
      stage: analysis['stage'] as String?,
      feedback: analysis['feedback'] as String?,
      error: analysis['error'] as String?,
      angles: angles,
    );
  }
}

class LandingPage extends StatefulWidget {
  const LandingPage({super.key});

  @override
  State<LandingPage> createState() => _LandingPageState();
}

class _LandingPageState extends State<LandingPage> with WidgetsBindingObserver {
  StreamSubscription<String>? _landmarkSubscription;
  PoseResult _result = PoseResult.empty;
  String? _error;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _landmarkSubscription = NativePoseBridge.landmarks.listen(
      _applyPayload,
      onError: (Object error) {
        if (mounted) setState(() => _error = error.toString());
      },
    );
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _landmarkSubscription?.cancel();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    // Returning from the native CameraActivity: fetch the latest result.
    if (state == AppLifecycleState.resumed) {
      NativePoseBridge.lastResult().then((payload) {
        if (payload != null) _applyPayload(payload);
      }).catchError((Object _) {});
    }
  }

  void _applyPayload(String payload) {
    try {
      final result = PoseResult.fromJson(payload);
      if (!mounted) return;
      setState(() => _result = result);
    } on FormatException catch (exception) {
      debugPrint('Invalid pose payload: $exception');
    }
  }

  Future<void> _openCamera() async {
    setState(() => _error = null);
    try {
      await NativePoseBridge.openCamera();
    } on PlatformException catch (exception) {
      if (mounted) {
        setState(() => _error = exception.message ?? 'Unable to open the camera.');
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(title: const Text('Gym Assistant')),
      body: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(Icons.accessibility_new, size: 72),
              const SizedBox(height: 20),
              Text(
                'Real-time pose analysis',
                style: theme.textTheme.headlineSmall,
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 10),
              const Text(
                'Open the camera to detect body landmarks with MediaPipe.',
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 28),
              FilledButton.icon(
                onPressed: _openCamera,
                icon: const Icon(Icons.camera_alt),
                label: const Text('Activate camera'),
              ),
              const SizedBox(height: 20),
              Text('Landmarks received: ${_result.landmarkCount}'),
              Text('Squat reps: ${_result.reps}', style: theme.textTheme.titleMedium),
              if (_result.stage != null) Text('Stage: ${_result.stage}'),
              if (_result.feedback != null) Text(_result.feedback!),
              if (_result.angles.isNotEmpty) ...[
                const SizedBox(height: 8),
                for (final entry in _result.angles.entries)
                  Text('${entry.key}: ${entry.value.toStringAsFixed(0)}°',
                      style: theme.textTheme.bodySmall),
              ],
              if (_result.error != null || _error != null) ...[
                const SizedBox(height: 12),
                Text(_error ?? _result.error!,
                    style: TextStyle(color: theme.colorScheme.error)),
              ],
            ],
          ),
        ),
      ),
    );
  }
}

class NativePoseBridge {
  static const _channel = MethodChannel('com.example.gym_assistant/pose');
  static const _landmarkChannel = EventChannel('com.example.gym_assistant/landmarks');

  static Stream<String> get landmarks =>
      _landmarkChannel.receiveBroadcastStream().map((event) => event as String);

  static Future<void> openCamera() async {
    await _channel.invokeMethod<void>('openCamera');
  }

  static Future<String?> lastResult() => _channel.invokeMethod<String>('getLastResult');
}
