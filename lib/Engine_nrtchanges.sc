Engine_NRTchanges : CroneEngine {

	var params;
	var voice;

	alloc {

		// add SynthDefs
		SynthDef("NRTchanges", {
			arg out = 0,
			freq, cutoff,
			kill_gate = 1;

			var pulse = Pulse.ar(freq: freq);
			var saw = Saw.ar(freq: freq);
			var sub = Pulse.ar(freq: freq/2);
			var noise = WhiteNoise.ar(mul: 0.25);
			var mix = Mix.ar([pulse,saw,sub,noise]);

			var envelope = EnvGen.kr(envelope: Env.perc(attackTime: 0, releaseTime: 15, level: 0.5),gate: kill_gate);
      var kill_when_dead = Env.perc(attackTime: 0, releaseTime: 15, level: 0.5).kr(doneAction:2);

			var slewedcut = Lag2.kr(cutoff,0.1);
			var filter = MoogFF.ar(in: mix, freq: slewedcut, gain: 3);

			var signal = Pan2.ar(filter*envelope,0);

			Out.ar(out,signal);

		}).add;

		Server.default.sync;

		params = Dictionary.newFrom([
			\cutoff, 8000,
		]);

		params.keysDo({ arg key;
			this.addCommand(key, "f", { arg msg;
				params[key] = msg[1];
				if (voice == nil,{
				},{
					if (voice.isRunning,{
						("setting "++(params[key])).postln;
						voice.set(params[key],msg[1]);
					});
				});
			});
		});

		this.addCommand("hz", "f", { arg msg;
			if (voice == nil,{
				},{
				if (voice.isRunning,{
					voice.set(\kill_gate,-1.05);
				});
			});
			voice = Synth("NRTchanges", [\freq, msg[1]] ++ params.getPairs);
			NodeWatcher.register(voice);
		});

	}

}