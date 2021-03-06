package de.timmeey.anoBitT.communication;

import com.google.gson.Gson;

import de.timmeey.libTimmeey.networking.NetSerializer;

public class NetSerializerImpl implements NetSerializer {
	private final Gson gson;

	protected NetSerializerImpl(Gson gson) {
		this.gson = gson;
	}

	@Override
	public String toJson(Object object) {
		return gson.toJson(object);
	}

	@Override
	public <T> T fromJson(String string, Class<T> clazz) {
		return gson.fromJson(string, clazz);
	}

}
