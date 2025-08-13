package kr.Windmill.config;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import kr.Windmill.util.Common;

@WebListener
public class ApplicationShutdownListener implements ServletContextListener {

	private static final Logger logger = LoggerFactory.getLogger(ApplicationShutdownListener.class);

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		logger.info("Windmill 애플리케이션이 시작되었습니다.");
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		System.out.println("=== Windmill 애플리케이션 종료 시작 ===");

		long startTime = System.currentTimeMillis();

		try {

			// Spring 컨텍스트 가져오기
			WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(sce.getServletContext());

			if (context != null) {
				System.out.println("Spring 컨텍스트에서 서비스 정리 시작...");

				// Spring 컨텍스트 종료 전에 수동으로 서비스들을 정리
				cleanupSpringServices(context);

				// 최대 1초 대기 (실제로는 1ms만에 완료되므로 충분)
				try {
					Thread.sleep(1000);
					System.out.println("서비스 정리 대기 완료 (1초)");
				} catch (InterruptedException e) {
					System.out.println("서비스 정리 대기 중 인터럽트 발생");
					Thread.currentThread().interrupt();
				}
			} else {
				System.out.println("Spring 컨텍스트를 찾을 수 없습니다.");
			}

			// Common 클래스의 정리 작업 실행
			try {
				Common.cleanupOnShutdown();
				System.out.println("Common 클래스 정리 작업 완료");
			} catch (Exception e) {
				System.out.println("Common 클래스 정리 작업 중 오류 발생: " + e.getMessage());
			}

			// 추가 정리 작업
			cleanupResources();

			long endTime = System.currentTimeMillis();
			long duration = endTime - startTime;

			System.out.println("=== Windmill 애플리케이션 종료 완료 ===");
			System.out.println("총 정리 시간: " + duration + "ms");

		} catch (Exception e) {
			System.out.println("=== 애플리케이션 종료 중 치명적 오류 발생 ===");
			e.printStackTrace();
		}
	}

	private void cleanupSpringServices(WebApplicationContext context) {
		try {
			// DynamicJdbcManager 정리
			try {
				Object dynamicJdbcManager = context.getBean("dynamicJdbcManager");
				if (dynamicJdbcManager != null) {
					if (dynamicJdbcManager instanceof AutoCloseable) {
						((AutoCloseable) dynamicJdbcManager).close();
						System.out.println("1. DynamicJdbcManager 정리 완료");
					}
				}
			} catch (Exception e) {
				System.out.println("DynamicJdbcManager 정리 중 오류: " + e.getMessage());
			}

			// ConnectionService 정리
			try {
				Object connectionService = context.getBean("connectionService");
				if (connectionService != null) {
					if (connectionService instanceof AutoCloseable) {
						((AutoCloseable) connectionService).close();
						System.out.println("2. ConnectionService 정리 완료");
					}
				}
			} catch (Exception e) {
				System.out.println("ConnectionService 정리 중 오류: " + e.getMessage());
			}

			// DexStatusService 정리
			try {
				Object dexStatusService = context.getBean("dexStatusService");
				if (dexStatusService != null) {
					if (dexStatusService instanceof AutoCloseable) {
						((AutoCloseable) dexStatusService).close();
						System.out.println("3. DexStatusService 정리 완료");
					}
				}
			} catch (Exception e) {
				System.out.println("DexStatusService 정리 중 오류: " + e.getMessage());
			}

			System.out.println("Spring 서비스 수동 정리 완료");

		} catch (Exception e) {
			System.out.println("Spring 서비스 정리 중 오류: " + e.getMessage());
		}
	}

	private void cleanupResources() {
		System.out.println("=== 추가 리소스 정리 시작 ===");

		try {
			// 메모리 정리 (가장 효과적)
			cleanupMemory();

			System.out.println("=== 추가 리소스 정리 완료 ===");

		} catch (Exception e) {
			System.out.println("추가 리소스 정리 중 오류: " + e.getMessage());
		}
	}

	private void cleanupMemory() {
		try {
			System.out.println("메모리 정리 시작...");

			// 가비지 컬렉션 요청
			long beforeMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			System.gc();

			// 잠시 대기
			try {
				Thread.sleep(500); // 1초 → 500ms로 단축
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}

			long afterMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			long freedMemory = beforeMemory - afterMemory;

			System.out.println("메모리 정리 완료 - 해제된 메모리: " + freedMemory + " bytes (" + String.format("%.2f", freedMemory / (1024.0 * 1024.0)) + " MB)");

		} catch (Exception e) {
			System.out.println("메모리 정리 중 오류: " + e.getMessage());
		}
	}
}