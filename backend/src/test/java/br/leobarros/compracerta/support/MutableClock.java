package br.leobarros.compracerta.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class MutableClock extends Clock {

	private Instant instant;

	public MutableClock(Instant instant) {
		this.instant = instant;
	}

	public void avancar(Duration duration) {
		instant = instant.plus(duration);
	}

	@Override
	public ZoneId getZone() {
		return ZoneOffset.UTC;
	}

	@Override
	public Clock withZone(ZoneId zone) {
		return this;
	}

	@Override
	public Instant instant() {
		return instant;
	}
}
