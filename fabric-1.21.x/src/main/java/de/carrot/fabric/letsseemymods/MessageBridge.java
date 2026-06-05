package de.carrot.fabric.letsseemymods;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

final class MessageBridge {
	private MessageBridge() {
	}

	static void sendSuccess(CommandSourceStack source, Component message, boolean broadcastToAdmins) {
		source.sendSuccess(() -> message, broadcastToAdmins);
	}
}
