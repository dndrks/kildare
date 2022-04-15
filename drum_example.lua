engine.name = 'drumexample'
s = require 'sequins'

kildare_setup = include 'lib/kildare'

function init()
  kildare_setup.init()
  sync_vals = s{1,1/3,1/2,1/6,2}
  cb_pitches = s{404,202,606,101,75}
  sd_pitches = s{404,202,606,101,75}
  rs_SPTCH = s{1,2,3,4,5,6,7,8,9,10}

  clock.run(iter)
  clock.run(iter_rim)

  engine.hh_modAmp(127)
  engine.hh_modRel(0.3)
  engine.hh_modHz(100)
  engine.hh_carHz(100)
  engine.hh_tremHz(10)
  engine.hh_tremDepth(1)
  engine.hh_pan(0)
  engine.hh_carRel(0.3)
  engine.hh_amp(0.25)
  -- engine.bd_LPfreq(200)
  engine.cb_HPfreq(30)
  -- engine.sd_carRel(0.1)
  engine.rs_sdAtk(0.2)
  engine.rs_carHz(400)
  engine.rs_brate(12000)
  engine.rs_bcnt(8)
  engine.rs_SCHNK(2)
  -- engine.bd_SCHNK(3)
  -- engine.bd_SPTCH(1)
  
  clock.run(function() while true do clock.sync(1/3) if math.random(100)<30 then engine.trig_bd() end end end)
end

function iter_rim()
  while true do
    clock.sync(1/5)
    if math.random(100) > 30 then
      engine.rs_LPfreq(math.random(1000,3000))
      engine.rs_HPfreq(math.random(1000,3000))
      engine.rs_pan(math.random(-80,80)/100)
      -- engine.trig_rs()
    end
    if math.random(100) < 30 then
      engine.rs_SPTCH(rs_SPTCH())
    end
  end
end

function iter()
  while true do
    clock.sync(sync_vals())
    if math.random(100) > 50 then
      -- engine.bd_pan(math.random(-50,50)/100)
      engine.hh_pan(math.random(-100,100)/100)
      engine.hh_tremHz(math.random(10))
      -- engine.bd_modHz(math.random(30,3000))
      engine.sd_brate(math.random(8000,24000))
      engine.cb_carRel(math.random(10,1000)/1000)
      engine.cb_carHz(cb_pitches())
      engine.cb_pan(math.random(-100,100)/100)
      -- engine.trig_hh()
      engine.cb_HPfreq(math.random(30,12000))
      engine.sd_carHz(math.random(287,300))
    end
    engine.sd_pan(math.random(-30,30)/100)
    engine.sd_carRel(math.random(1,200)/100)
    engine.sd_carHz(sd_pitches() * math.random(-3,3))
    engine.trig_sd()
    if math.random(100)>30 then
      engine.trig_cb()
    end
  end
end