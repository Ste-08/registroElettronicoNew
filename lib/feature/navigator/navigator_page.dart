import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:pedantic/pedantic.dart';
import 'package:registro_elettronico/core/infrastructure/app_injection.dart';
import 'package:registro_elettronico/core/infrastructure/localizations/app_localizations.dart';
import 'package:registro_elettronico/feature/agenda/presentation/agenda_page.dart';
import 'package:registro_elettronico/feature/grades/presentation/grades_page.dart';
import 'package:registro_elettronico/feature/home/home_page.dart';
import 'package:registro_elettronico/feature/navigator/more_page.dart';
import 'package:registro_elettronico/feature/noticeboard/presentation/noticeboard_page.dart';
import 'package:registro_elettronico/utils/update_manager.dart';

class NavigatorPage extends StatefulWidget {
  final bool fromLogin;

  NavigatorPage({
    Key key,
    this.fromLogin = false,
  }) : super(key: key);

  @override
  _NavigatorPageState createState() => _NavigatorPageState();
}

class _NavigatorPageState extends State<NavigatorPage> with WidgetsBindingObserver {
  static const MethodChannel _widgetChannel =
      MethodChannel('com.riccardocalligaro.registro_elettronico/agenda_widget');
  
  int _currentPage = 0;
  List<Widget> _pages;
  SRUpdateManager srUpdateManager;

  static const int home = 0;
  static const int grades = 1;
  static const int agenda = 2;
  static const int noticeboard = 3;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    srUpdateManager = sl();
    unawaited(srUpdateManager.checkForUpdates());

    _pages = [
      HomePage(
        fromLogin: widget.fromLogin,
      ),
      GradesPage(),
      AgendaPage(),
      NoticeboardPage(),
      MorePage(),
    ];
    
    // Check if opened from widget
    _checkWidgetNavigation();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _checkWidgetNavigation();
    }
  }

  Future<void> _checkWidgetNavigation() async {
    try {
      final String navigation = await _widgetChannel.invokeMethod('getPendingNavigation');
      if (navigation == 'agenda') {
        setState(() {
          _currentPage = agenda;
        });
      }
    } catch (e) {
      // Ignore - widget channel might not be available
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      bottomNavigationBar: _buildBottomNavigationBar(),
      body: IndexedStack(
        index: _currentPage,
        children: _pages,
      ),
    );
  }

  BottomNavigationBar _buildBottomNavigationBar() {
    return BottomNavigationBar(
      backgroundColor: Theme.of(context).cardTheme.color,
      type: BottomNavigationBarType.fixed,
      currentIndex: _currentPage,
      showSelectedLabels: false,
      showUnselectedLabels: false,
      onTap: (int index) {
        if (_currentPage == index) {
          if (index == home) {
            if (homeRefresherKey.currentState != null) {
              homeRefresherKey.currentState.show();
            }
          } else if (index == agenda) {
            srUpdateManager.updateAgendaData(context);
          } else if (index == grades) {
            if (gradesRefresherKey.currentState != null) {
              gradesRefresherKey.currentState.show();
            }
          } else if (index == noticeboard) {
            if (noticeboardRefresherKey.currentState != null) {
              noticeboardRefresherKey.currentState.show();
            }
          }
        }
        setState(() {
          _currentPage = index;
        });
      },
      items: [
        BottomNavigationBarItem(
          label: AppLocalizations.of(context).translate('home'),
          icon: Icon(Icons.home),
        ),
        BottomNavigationBarItem(
          label: AppLocalizations.of(context).translate('grades'),
          icon: Icon(
            Icons.class_,
          ),
        ),
        BottomNavigationBarItem(
          label: AppLocalizations.of(context).translate('agenda'),
          icon: Icon(
            Icons.today,
            size: 25,
          ),
        ),
        BottomNavigationBarItem(
          label: AppLocalizations.of(context).translate('notice_board'),
          icon: Icon(Icons.email),
        ),
        BottomNavigationBarItem(
          label: AppLocalizations.of(context).translate('more_page'),
          icon: Icon(Icons.more_horiz),
        ),
      ],
    );
  }

  void goToGradesPage() {
    setState(() {
      _currentPage = grades;
    });
  }
}
