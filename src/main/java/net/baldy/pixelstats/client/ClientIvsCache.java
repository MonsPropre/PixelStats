package net.baldy.pixelstats.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientIvsCache {

	private static final Map<UUID, IvData> CACHE = new ConcurrentHashMap<>();
	private static final long REFRESH_MS = 5000L;

	public static IvData get(UUID uuid) {
		return CACHE.get(uuid);
	}

	public static void put(UUID uuid, IvData data) {
		CACHE.put(uuid, data);
	}

	public static boolean hasData(UUID uuid) {
		IvData data = CACHE.get(uuid);
		return data != null;
	}

	public static boolean shouldRequest(UUID uuid) {
		IvData data = CACHE.get(uuid);

		if (data == null) {
			return true;
		}

		if (data.requesting) {
			return false;
		}

		return System.currentTimeMillis() - data.timestamp >= REFRESH_MS;
	}

	public static void markRequesting(UUID uuid) {
		CACHE.compute(uuid, (key, old) -> {
			if (old == null) {
				return new IvData(false, 0, false, 0L, true);
			}

			return new IvData(old.valid, old.total, old.hyperTrained, old.timestamp, true);
		});
	}

	public static final class IvData {
		public final boolean valid;
		public final int total;

		public final boolean hyperTrained;
		public final long timestamp;
		public final boolean requesting;

		public IvData(boolean valid, int total, boolean hyperTrained, long timestamp, boolean requesting) {
			this.valid = valid;
			this.total = total;
			this.hyperTrained = hyperTrained;
			this.timestamp = timestamp;
			this.requesting = requesting;
		}
	}

	private ClientIvsCache() {
	}
}