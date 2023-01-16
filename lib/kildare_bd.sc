KildareBD {

	*new {
		arg srv;
		^super.new.init(srv);
	}

	/**buildParams {
		arg mainOutBus, delayLSendBus, delayRSendBus, feedbackSendBus;
		Dictionary.newFrom([
			\out,mainOutBus,
			\delayAuxL,delayLSendBus,
			\delayAuxR,delayRSendBus,
			\feedbackAux,feedbackSendBus,
			\delayEnv,0,
			\delayAtk,0,
			\delayRel,2,
			\delayCurve,-4,
			\delaySend,0,
			\feedbackEnv,0,
			\feedbackAtk,0,
			\feedbackRel,2,
			\feedbackCurve,-4,
			\feedbackSend,0,
			\poly,0,
			\velocity,127,
			\amp,0.7,
			\carHz,55,
			\carHzThird,55,
			\carHzSeventh,55,
			\carDetune,0,
			\carAtk,0,
			\carRel,0.3,
			\carCurve,-4,
			\modAmp,0,
			\modHz,600,
			\modFollow,0,
			\modNum,1,
			\modDenum,1,
			\modAtk,0,
			\modRel,0.05,
			\modCurve,-4,
			\feedAmp,1,
			\rampDepth,0.11,
			\rampDec,0.3,
			\squishPitch,1,
			\squishChunk,1,
			\amDepth,0,
			\amHz,8175.08,
			\eqHz,6000,
			\eqAmp,0,
			\bitRate,24000,
			\bitCount,24,
			\lpHz,20000,
			\hpHz,20,
			\filterQ,50,
			\lpAtk,0,
			\lpRel,0.3,
			\lpCurve,-4,
			\lpDepth,0,
			\pan,0,
		]);
	}*/

	init {

		SynthDef(\kildare_bd, {
			arg out = 0, t_gate = 0,
			delayAuxL, delayAuxR, delaySend,
			delayEnv, delayAtk, delayRel, delayCurve = -4,
			feedbackAux, feedbackSend,
			feedbackEnv, feedbackAtk, feedbackRel, feedbackCurve = -4,
			velocity = 127, amp,
			carHz, carHzThird, carHzSeventh,
			carDetune, carAtk, carRel, carCurve = -4,
			modHz, modAmp, modAtk, modRel, modCurve = -4, feedAmp,
			modFollow, modNum, modDenum,
			pan, rampDepth, rampDec,
			squishPitch, squishChunk,
			amDepth, amHz,
			eqHz, eqAmp, bitRate, bitCount,
			lpHz, hpHz, filterQ,
			lpAtk, lpRel, lpCurve = -4, lpDepth;

			var car, carThird, carSeventh,
			mod,
			carEnv, modEnv, carRamp,
			feedMod, feedCar, ampMod, click, clicksound,
			mod_1, mod_2, mod_3,
			filterEnv, delEnv, feedEnv, mainSend;

			eqHz = eqHz.lag3(0.1);
			lpHz = lpHz.lag3(0.1);
			hpHz = hpHz.lag3(0.1);
			delaySend = delaySend.lag3(0.1);
			feedbackSend = feedbackSend.lag3(0.1);
			modHz = (modHz * (1 - modFollow)) + (carHz * modFollow * modDenum);

			filterQ = LinLin.kr(filterQ,0,100,1.0,0.001);
			modAmp = LinLin.kr(modAmp,0.0,1.0,0,127).lag3(0.1);
			feedAmp = LinLin.kr(feedAmp,0.0,1.0,0.0,10.0);
			eqAmp = LinLin.kr(eqAmp,-2.0,2.0,-10.0,10.0);
			rampDepth = LinLin.kr(rampDepth,0.0,1.0,0.0,2.0);
			amDepth = LinLin.kr(amDepth,0.0,1.0,0.0,2.0);
			carHz = (carHz * (1 - modFollow)) + (carHz * modFollow * modNum);
			carHz = carHz * (2.pow(carDetune/12));

			modEnv = EnvGen.kr(
				envelope: Env.new([0,0,1,0], times: [0.01,modAtk,modRel], curve: [modCurve,modCurve*(-1)]),
				gate: t_gate
			);
			filterEnv = EnvGen.kr(
				envelope: Env.new([0,0,1,0], times: [0.01,lpAtk,lpRel], curve: [lpCurve,lpCurve*(-1)]),
				gate: t_gate
			);
			carRamp = EnvGen.kr(
				Env([1000,1000, 0.000001], [0,rampDec], curve: \exp),
				gate: t_gate
			);
			carEnv = EnvGen.kr(
				envelope: Env.new([0,0,1,0], times: [0,carAtk,carRel], curve: [carCurve,carCurve*(-1)]),
				gate: t_gate
			);

			mod_1 = SinOscFB.ar(
				modHz + ((carRamp*3)*rampDepth),
				feedAmp,
				modAmp*10
			)* modEnv;

			car = SinOsc.ar(carHz + (mod_1) + (carRamp*rampDepth));
			car = car*carEnv;

			ampMod = SinOsc.ar(freq:amHz,mul:(amDepth/2),add:1);

			click = amp/4;
			clicksound = LPF.ar(Impulse.ar(0.003),16000,click) * EnvGen.kr(
				envelope: Env.new([0,0,1,0], times: [0.0,carAtk,0.2]),
				gate: t_gate
			);
			car = (car + clicksound)* ampMod;

			car = Squiz.ar(in:car, pitchratio:squishPitch, zcperchunk:squishChunk, mul:1);
			car = Decimator.ar(car,bitRate,bitCount,1.0);
			car = BPeakEQ.ar(in:car,freq:eqHz,rq:1,db:eqAmp,mul:1);
			car = RLPF.ar(in:car,freq:Clip.kr(lpHz + ((5*(lpHz * filterEnv)) * lpDepth), 20, 20000), rq: filterQ, mul:1);
			car = RHPF.ar(in:car,freq:hpHz, rq: filterQ, mul:1);
			car = Compander.ar(in:car, control:car, thresh:0.3, slopeBelow:1, slopeAbove:0.1, clampTime:0.01, relaxTime:0.01);

			mainSend = Pan2.ar(car,pan);
			mainSend = mainSend * (amp * LinLin.kr(velocity,0,127,0.0,1.0));

			delEnv = Select.kr(
				delayEnv > 0, [
					delaySend,
					delaySend * EnvGen.kr(
						envelope: Env.new([0,0,1,0], times: [0.01,delayAtk,delayRel], curve: [delayCurve,delayCurve*(-1)]),
						gate: t_gate
					)
				]
			);

			feedEnv = Select.kr(
				feedbackEnv > 0, [
					feedbackSend,
					feedbackSend * EnvGen.kr(
						envelope: Env.new([0,0,1,0], times: [0.01,feedbackAtk,feedbackRel], curve: [feedbackCurve,feedbackCurve*(-1)]),
						gate: t_gate
					)
				]
			);

			Out.ar(out, mainSend);
			Out.ar(delayAuxL, (car * amp * LinLin.kr(velocity,0,127,0.0,1.0) * delEnv));
			Out.ar(delayAuxR, (car * amp * LinLin.kr(velocity,0,127,0.0,1.0) * delEnv));
			Out.ar(feedbackAux, (mainSend * (feedbackSend * feedEnv)));
		}).send;
	}
}