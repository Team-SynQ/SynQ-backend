package com.synq.backend.domain.reference.link;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import okhttp3.Dns;

/**
 * 사용자가 준 URL 로 서버가 요청을 보내는 기능(SSRF)의 방어선.
 *
 * <p>URL 문자열 검사로는 막을 수 없다. 공인 도메인의 A 레코드가 내부 IP 를 가리킬 수 있기 때문이다.
 * 그렇다고 "미리 해석해서 검사한 뒤 요청" 하면 해석이 두 번 일어나 그 사이에 DNS 가 바뀌는
 * rebinding 에 뚫린다.
 *
 * <p>그래서 OkHttp 의 DNS 훅 자리에 끼운다. OkHttp 는 커넥션을 맺기 직전에 이 메서드를 호출하고
 * 반환된 주소로 바로 접속하므로, 검사와 접속이 한 번의 해석을 공유한다.
 * 리다이렉트도 새 호스트마다 이 메서드를 다시 타므로 체인 전체가 덮인다.
 */
public class SafeDns implements Dns {

	private final Dns delegate;
	private final boolean allowPrivateNetwork;

	public SafeDns(boolean allowPrivateNetwork) {
		this(Dns.SYSTEM, allowPrivateNetwork);
	}

	/** 실제 DNS 를 타지 않고 검사 로직만 확인하기 위한 생성자. */
	SafeDns(Dns delegate, boolean allowPrivateNetwork) {
		this.delegate = delegate;
		this.allowPrivateNetwork = allowPrivateNetwork;
	}

	@Override
	public List<InetAddress> lookup(String hostname) throws UnknownHostException {
		List<InetAddress> resolved = delegate.lookup(hostname);
		if (allowPrivateNetwork) {
			return resolved;
		}
		for (InetAddress address : resolved) {
			if (isBlocked(address)) {
				throw new BlockedAddressException(hostname);
			}
		}
		return resolved;
	}

	private boolean isBlocked(InetAddress address) {
		return address.isLoopbackAddress()
				|| address.isLinkLocalAddress()
				|| address.isSiteLocalAddress()
				|| address.isAnyLocalAddress()
				|| address.isMulticastAddress()
				|| isUniqueLocalIpv6(address);
	}

	/** fc00::/7. isSiteLocalAddress() 는 이 대역을 잡지 못한다. */
	private boolean isUniqueLocalIpv6(InetAddress address) {
		if (!(address instanceof Inet6Address)) {
			return false;
		}
		return (address.getAddress()[0] & 0xFE) == 0xFC;
	}
}
