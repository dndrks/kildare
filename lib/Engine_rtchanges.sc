Engine_RTchanges : CroneEngine {

	var params;
	var voice;
	var buscontrol;

	alloc {

		// add SynthDefs
		SynthDef("RTchanges", {
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

			var slewed_cut = In.kr(cutoff).lag2(0.1);
			var filter = MoogFF.ar(in: mix, freq: slewed_cut, gain: 3);

			var signal = Pan2.ar(filter*envelope,0);

			Out.ar(out,signal);

		}).add;

		buscontrol = Bus('control',numChannels:1);
		buscontrol.set(8000);

		Server.default.sync;

/*		params = Dictionary.newFrom([
			\cutoff, buscontrol,
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
		});*/

		this.addCommand("hz", "f", { arg msg;
			if (voice == nil,{
				},{
				if (voice.isRunning,{
					voice.set(\kill_gate,-1.05);
				});
			});
			voice = Synth("RTchanges", [\freq, msg[1], \cutoff, buscontrol]);
			NodeWatcher.register(voice);
		});

		this.addCommand("modulating_cutoff", "f", { arg msg;
			if (voice == nil,{
				},{
				if (voice.isRunning,{
					("setting "++(msg[1])).postln;
					buscontrol.set(msg[1]);
				});
			});
		});

	}

}