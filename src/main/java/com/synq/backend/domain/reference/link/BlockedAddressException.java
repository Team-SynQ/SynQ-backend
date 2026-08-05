package com.synq.backend.domain.reference.link;

import java.net.UnknownHostException;

/**
 * SafeDns 가 사설·내부 대역을 거부할 때 던진다.
 *
 * <p>{@link okhttp3.Dns#lookup} 이 {@link UnknownHostException} 만 던질 수 있어 그 하위 타입으로 만든다.
 * 호출부는 이 타입 여부로 "차단됨(400 LINK_ADDRESS_NOT_ALLOWED)" 과 "도달 불가(400 LINK_UNREACHABLE)" 를 가른다.
 */
public class BlockedAddressException extends UnknownHostException {

	public BlockedAddressException(String hostname) {
		super("허용되지 않는 주소입니다: " + hostname);
	}
}
