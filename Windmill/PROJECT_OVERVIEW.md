# **Windmill 프로젝트 개요**

## **🎯 프로젝트 목적**
DeX(Data Exchange) 시스템의 실시간 모니터링을 위한 웹 기반 대시보드

## **🚀 핵심 기능**
- **실시간 DB 연결 상태 모니터링**
- **성능 지표 시각화 (4개 차트)**
- **DEX 서비스 상태 확인**
- **해시 기반 성능 최적화**

## **💻 기술 스택**
- **Backend**: Java Spring + Tomcat 9
- **Frontend**: JSP + JavaScript + Chart.js + Bootstrap
- **Database**: Oracle, PostgreSQL, Tibero 지원

## **📊 성능 최적화 성과**
- Chart.js 활동 **61% 감소**
- 차트 업데이트 **45% 감소**
- 메모리 사용량 **50-60% 감소**

## **🏗️ 아키텍처**
```
Controller → Service → Mapper
    ↓
JSP View ← JavaScript ← AJAX
    ↓
Chart.js + Bootstrap UI
```

## **📁 주요 파일**
- `DashboardController.java` - 대시보드 API
- `dashboard.jsp` - 메인 대시보드 페이지
- `SQLExecuteService.java` - SQL 실행 서비스

## **🔧 배포**
- Maven WAR 파일 생성
- Tomcat webapps/ROOT.war 배포
- 수동 배포 방식

## **🎨 UI 특징**
- 반응형 Bootstrap 디자인
- 실시간 자동 새로고침 (10초)
- 색상 코딩 상태 표시
- 직관적인 카드/차트 레이아웃

---
**Windmill은 DeX 시스템 운영을 위한 필수 모니터링 도구입니다.**
