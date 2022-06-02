Engine_Kildare : CroneEngine {
	var kernel;

	*new { arg context, doneCallback;
		^super.new(context, doneCallback);
	}

	alloc {
		kernel = Kildare.new(Crone.server);

		this.addCommand(\trig, "s", { arg msg;
			var k = msg[1].asSymbol;
			kernel.trigger(k);
		});

		this.addCommand(\set_param, "ssf", { arg msg;
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
	}

	free {
		kernel.free;
	}
}