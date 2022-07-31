Engine_Kildare : CroneEngine {
	var kernel, debugPrinter;

	*new { arg context, doneCallback;
		^super.new(context, doneCallback);
	}

	alloc {
		kernel = Kildare.new(Crone.server);

		this.addCommand(\trig, "sf", { arg msg;
			var k = msg[1].asSymbol;
			var velocity = msg[2].asFloat;
			kernel.trigger(k,velocity);
		});

		this.addCommand(\set_voice_param, "ssf", { arg msg;
			var voiceKey = msg[1].asSymbol;
			var paramKey = msg[2].asSymbol;
			var paramValue = msg[3].asFloat;
			kernel.setVoiceParam(voiceKey, paramKey, paramValue);
		});

		this.addCommand(\set_delay_param, "sf", {arg msg;
			var paramKey = msg[1].asSymbol;
			var paramValue = msg[2].asFloat;
			kernel.setDelayParam(paramKey, paramValue);
		});

		this.addCommand(\set_reverb_param, "sf", {arg msg;
			var paramKey = msg[1].asSymbol;
			var paramValue = msg[2].asFloat;
			kernel.setReverbParam(paramKey, paramValue);
		});

		this.addCommand(\set_main_param, "sf", {arg msg;
			var paramKey = msg[1].asSymbol;
			var paramValue = msg[2].asFloat;
			kernel.setMainParam(paramKey, paramValue);
		});

		debugPrinter = { loop { [context.server.peakCPU, context.server.avgCPU].postln; 3.wait; } }.fork;
	}

	free {
		kernel.free;
		debugPrinter.stop;
	}
}