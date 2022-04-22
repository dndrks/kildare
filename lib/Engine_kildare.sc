Engine_Kildare : CroneEngine {
	var kernel;

	*new { arg context, doneCallback;
		^super.new(context, doneCallback);
	}

	alloc {
		kernel = Kildare.new(Crone.server);

		// one way:
		/*kernel.sendKeys.do({ arg voiceKey;
			this.addCommand(\trigger_++voiceKey, "", {
				kernel.trigger(voiceKey);
			});
			this.addCommand(\param_++voiceKey, "sf", { arg msg;
				kernel.setVoiceParam(voiceKey, msg[1].asSymbol, msg[2].asFloat);
			});
		});*/

		// another way:
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
	}

	free {
		kernel.free;
	}
}