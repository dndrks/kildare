Engine_Kildare : CroneEngine {
	var kernel, debugPrinter;

	*new { arg context, doneCallback;
		^super.new(context, doneCallback);
	}

	alloc {
		kernel = Kildare.new(Crone.server);

		this.addCommand(\trig, "sfs", { arg msg;
			var k = msg[1].asSymbol;
			var velocity = msg[2].asFloat;
			var retrigFlag = msg[3].asSymbol;
			kernel.test_trigger(k,velocity);
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

		this.addCommand(\set_feedback_param, "ssf", {arg msg;
			var targetKey = msg[1].asSymbol;
			var paramKey = msg[2].asSymbol;
			var paramValue = msg[3].asFloat;
			kernel.setFeedbackParam(targetKey, paramKey, paramValue);
		});

		this.addCommand(\set_main_param, "sf", {arg msg;
			var paramKey = msg[1].asSymbol;
			var paramValue = msg[2].asFloat;
			kernel.setMainParam(paramKey, paramValue);
		});

		this.addCommand(\load_file, "ss", { arg msg;
			kernel.loadFile(msg);
		});

		this.addCommand(\load_folder, "ss", { arg msg;
			var voiceKey = msg[1].asSymbol;
			var filepath = msg[2].asSymbol;
			kernel.loadFolder(voiceKey, filepath);
		});

		this.addCommand(\change_sample, "si", { arg msg;
			var voiceKey = msg[1].asSymbol;
			var sample = msg[2].asInteger;
			kernel.setFile(voiceKey, sample);
		});

		this.addCommand(\stop_sample, "s", { arg msg;
			var voiceKey = msg[1].asSymbol;
			kernel.stopSample(voiceKey);
		});

		this.addCommand(\clear_samples, "s", { arg msg;
			var voiceKey = msg[1].asSymbol;
			kernel.clearSamples(voiceKey);
		});

		this.addCommand(\set_model, "sss", { arg msg;
			var voiceKey = msg[1].asSymbol;
			var synthKey = msg[2].asSymbol;
			var reseed = msg[3].asSymbol;
			kernel.setModel(voiceKey,synthKey,reseed);
		});

		this.addCommand(\free_voice, "s", { arg msg;
			var voiceKey = msg[1].asSymbol;
			kernel.freeVoice(voiceKey);
		});

		this.addCommand(\init_voice, "ss", { arg msg;
			var voiceKey = msg[1].asSymbol;
			var synthKey = msg[2].asSymbol;
			kernel.initVoice(voiceKey, synthKey);
		});

		this.addCommand(\set_voice_limit, "ii", { arg msg;
			var voice = msg[1].asSymbol;
			var limit = msg[2].asInteger;
			kernel.setVoiceLimit(voice, limit);
		});

		this.addCommand(\set_poly_param_style, "is", { arg msg;
			var voice = msg[1].asSymbol;
			var style = msg[2].asString;
			kernel.setPolyParamStyle(voice, style);
		});

		this.addCommand(\test_trig,"s", { arg msg;
			var k = msg[1].asSymbol;
			kernel.test_trigger(k);
		});

		// debugPrinter = { loop { [context.server.peakCPU, context.server.avgCPU].postln; 3.wait; } }.fork;
	}

	free {
		kernel.free;
		// debugPrinter.stop;
	}
}