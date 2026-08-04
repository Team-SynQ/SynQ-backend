package com.synq.backend.domain.reference.link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Dns;
import org.junit.jupiter.api.Test;

class SafeDnsTest {

	private static final String HOST = "example.test";

	@Test
	void 루프백_주소는_차단한다() {
		assertBlocked("127.0.0.1");
		assertBlocked("::1");
	}

	@Test
	void 링크로컬_주소는_차단한다() {
		// 클라우드 메타데이터 서비스. 이 기능에서 가장 위험한 대상이다.
		assertBlocked("169.254.169.254");
		assertBlocked("fe80::1");
	}

	@Test
	void 사설망_주소는_차단한다() {
		assertBlocked("10.0.0.1");
		assertBlocked("172.16.0.1");
		assertBlocked("192.168.0.1");
	}

	@Test
	void IPv6_유니크로컬_주소는_차단한다() {
		// isSiteLocalAddress() 가 fc00::/7 을 잡지 못해 별도 검사가 필요하다.
		assertBlocked("fc00::1");
		assertBlocked("fd12:3456::1");
	}

	@Test
	void 와일드카드와_멀티캐스트_주소는_차단한다() {
		assertBlocked("0.0.0.0");
		assertBlocked("224.0.0.1");
	}

	@Test
	void 공인_주소는_통과시킨다() throws Exception {
		SafeDns dns = new SafeDns(fixed("93.184.216.34"), false);

		assertThat(dns.lookup(HOST)).containsExactly(InetAddress.getByName("93.184.216.34"));
	}

	@Test
	void 공인과_사설이_섞여_있으면_차단한다() {
		// 커넥션 재시도가 사설 쪽으로 붙을 수 있으므로 하나라도 사설이면 거부한다.
		SafeDns dns = new SafeDns(fixed("93.184.216.34", "127.0.0.1"), false);

		assertThatThrownBy(() -> dns.lookup(HOST))
				.isInstanceOf(BlockedAddressException.class);
	}

	@Test
	void 사설망_허용_설정이면_통과시킨다() {
		// 테스트가 로컬 HTTP 서버를 쓰기 위한 스위치다. 프로덕션 기본값은 false 다.
		SafeDns dns = new SafeDns(fixed("127.0.0.1"), true);

		assertThatCode(() -> dns.lookup(HOST)).doesNotThrowAnyException();
	}

	@Test
	void 해석_자체가_실패하면_그대로_전파한다() {
		SafeDns dns = new SafeDns(hostname -> {
			throw new UnknownHostException(hostname);
		}, false);

		assertThatThrownBy(() -> dns.lookup(HOST))
				.isInstanceOf(UnknownHostException.class)
				.isNotInstanceOf(BlockedAddressException.class);
	}

	private void assertBlocked(String address) {
		SafeDns dns = new SafeDns(fixed(address), false);

		assertThatThrownBy(() -> dns.lookup(HOST))
				.isInstanceOf(BlockedAddressException.class);
	}

	/** 실제 DNS 를 타지 않도록 고정 주소를 돌려주는 대역. 리터럴 IP 는 조회 없이 파싱된다. */
	private Dns fixed(String... addresses) {
		return hostname -> {
			List<InetAddress> resolved = new ArrayList<>();
			for (String address : addresses) {
				resolved.add(InetAddress.getByName(address));
			}
			return resolved;
		};
	}
}
