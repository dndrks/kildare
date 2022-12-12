KildareHH {

	*new {
		arg srv;
		^super.new.init(srv);
	}

	init {
		SynthDef(\kildare_hh, {
			arg out = 0, t_gate = 0,
			delayAuxL, delayAuxR, delaySend,
			delayEnv, delayAtk, delayRel,
			feedbackAux,feedbackSend,
			velocity = 127, amp,
			carHz, carHzThird, carHzSeventh,
			carDetune, carAtk, carRel, carCurve = -4,
			tremDepth, tremHz,
			modAmp, modHz, modAtk, modRel, modCurve = -4,
			modFollow, modNum, modDenum,
			feedAmp,
			amDepth, amHz,
			eqHz, eqAmp,
			bitRate, bitCount,
			lpHz, hpHz, filterQ,
			lpAtk, lpRel, lpCurve = -4, lpDepth,
			pan,
			squishPitch, squishChunk;

			var car, carThird, carSeventh,
			modHzThird, modHzSeventh,
			mod_1, mod_2, mod_3,
			feedScale,
			carEnv, modEnv, tremolo, tremod,
			ampMod, filterEnv, delEnv, mainSend;

			amp = amp*0.85;
			eqHz = eqHz.lag3(0.1);
			lpHz = lpHz.lag3(0.1);
			hpHz = hpHz.lag3(0.1);
			delaySend = delaySend.lag3(0.1);
			feedbackSend = feedbackSend.lag3(0.1);

			filterQ = LinLin.kr(filterQ,0,100,1.0,0.001);
			modAmp = LinLin.kr(modAmp,0.0,1.0,0,127);
			feedAmp = LinLin.kr(feedAmp,0.0,1.0,0.0,6.0);
			eqAmp = LinLin.kr(eqAmp,-2.0,2.0,-10.0,10.0);
			tremDepth = LinLin.kr(tremDepth,0.0,100,0.0,1.0);
			amDepth = LinLin.kr(amDepth,0,1.0,0.0,2.0);

			carHz = carHz * (2.pow(carDetune/12));
			carHzThird = carHzThird * (2.pow(carDetune/12));
			carHzSeventh = carHzSeventh * (2.pow(carDetune/12));

			modHz = Select.kr(modFollow > 0, [modHz, carHz * (modNum / modDenum)]);
			modHzThird = Select.kr(modFollow > 0, [modHz, carHzThird * (modNum / modDenum)]);
			modHzSeventh = Select.kr(modFollow > 0, [modHz, carHzSeventh * (modNum / modDenum)]);

			modEnv = EnvGen.kr(
				envelope: Env.new([0,0,1,0], times: [0.0,modAtk,modRel], curve: [modCurve,modCurve*(-1)]),
				gate: t_gate
			);
			carEnv = EnvGen.kr(
				envelope: Env.new([0,0,1,0], times: [0.0,carAtk,carRel], curve: [carCurve,carCurve*(-1)]),
				gate: t_gate
			);
			filterEnv = EnvGen.kr(
				envelope: Env.new([0,0,1,0], times: [0.01,lpAtk,lpRel], curve: [lpCurve,lpCurve*(-1)]),
				gate: t_gate
			);

			ampMod = SinOsc.ar(freq:amHz,mul:amDepth,add:1);

			mod_1 = SinOsc.ar(modHz, mul:modAmp) * modEnv;
			mod_2 = SinOsc.ar(modHzThird, mul:modAmp) * modEnv;
			mod_3 = SinOsc.ar(modHzSeventh, mul:modAmp) * modEnv;

			car = SinOscFB.ar(carHz + mod_1, feedAmp) * carEnv * amp;
			carThird = SinOscFB.ar(carHzThird + mod_2, feedAmp) * carEnv * amp;
			carSeventh = SinOscFB.ar(carHzSeventh + mod_3, feedAmp) * carEnv * amp;

			car = (car * 0.5) + (carThird * 0.32) + (carSeventh * 0.18);

			feedScale = LinLin.kr(feedAmp,0,6,40,6600);
			car = HPF.ar(car,feedScale);
			car = car*ampMod;
			tremolo = SinOsc.ar(tremHz, 0, tremDepth);
			tremod = (1.0 - tremDepth) + tremolo;
			car = car*tremod;
			car = Squiz.ar(in:car, pitchratio:squishPitch, zcperchunk:squishChunk, mul:1);
			car = Decimator.ar(car,bitRate,bitCount,1.0);
			car = BPeakEQ.ar(in:car,freq:eqHz,rq:1,db:eqAmp,mul:1);
			car = RLPF.ar(in:car,freq:Clip.kr(lpHz + ((5*(lpHz * filterEnv)) * lpDepth), 20, 20000), rq: filterQ, mul:1);
			car = RHPF.ar(in:car,freq:hpHz, rq: filterQ, mul:1);

			car = Compander.ar(in:car,control:car, thresh:0.3, slopeBelow:1, slopeAbove:0.1, clampTime:0.01, relaxTime:0.01);
			mainSend = Pan2.ar(car,pan);
			mainSend = mainSend * (amp * LinLin.kr(velocity,0,127,0.0,1.0));

			delEnv = Select.kr(
				delayEnv > 0,[
					delaySend,
					(delaySend * EnvGen.kr(
						envelope: Env.new([0,0,1,0], times: [0.01,delayAtk,delayRel]),
						gate: t_gate)
					)
				]
			);

			Out.ar(out, mainSend);
			Out.ar(delayAuxL, (car * amp * LinLin.kr(velocity,0,127,0.0,1.0) * delEnv));
			Out.ar(delayAuxR, (car * amp * LinLin.kr(velocity,0,127,0.0,1.0) * delEnv));
			Out.ar(feedbackAux, (mainSend * feedbackSend));

		}).send;
	}
}