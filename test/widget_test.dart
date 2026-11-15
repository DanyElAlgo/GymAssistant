import 'package:flutter_test/flutter_test.dart';

import 'package:gym_assistant/main.dart';

void main() {
  testWidgets('landing page offers camera activation', (WidgetTester tester) async {
    await tester.pumpWidget(const MyApp());

    expect(find.text('Real-time pose analysis'), findsOneWidget);
    expect(find.text('Activate camera'), findsOneWidget);
    expect(find.text('Landmarks received: 0'), findsOneWidget);
    expect(find.text('Squat reps: 0'), findsOneWidget);
  });
}
