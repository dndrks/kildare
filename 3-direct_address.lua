engine.name = 'kildare'
s = require 'sequins'

kildare_setup = include 'lib/kildare'

function init()
  kildare_setup.init()
  sync_vals = s{1,1/3,1/2,1/6,2}
  cb_pitches = s{404,202,606,101,75}
  sd_pitches = s{404,202,606,101,75}
  rs_squish = s{1,2,3,4,5,6,7,8,9,10}
  
  engine.bd_amp(10) -- there's a limiter on each voice, which allows us to saturate with values outside of the normal parameters

  clock.run(iter)
  clock.run(iter_rs)
  clock.run(iter_rs_params)
  
  clock.run(function() while true do clock.sync(1/3) if math.random(100)<30 then engine.trig_bd() engine.trig_cb() end end end)
end

function iter_rs()
  while true do
    clock.sync(1/5)
    if math.random(100) > 30 then
      engine.trig_rs()
    end
  end
end

function iter_rs_params()
  while true do
    clock.sync(1/5)
    if math.random(100) > 30 then
      engine.rs_LPfreq(math.random(1000,3000))
      engine.rs_HPfreq(math.random(1000,3000))
      engine.rs_pan(math.random(-80,80)/100)
    end
    if math.random(100) < 30 then
      engine.rs_SPTCH(rs_squish())
    end
  end
end

function iter()
  while true do
    clock.sync(sync_vals())
    if math.random(100) > 50 then
      engine.sd_brate(math.random(8000,24000))
      engine.cb_carRel(math.random(10,1000)/1000)
      engine.cb_carHz(cb_pitches())
      engine.cb_pan(math.random(-100,100)/100)
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