engine.name = 'Kildare'
s = require 'sequins'

kildare_setup = include 'lib/kildare'

function init()
  kildare_setup.init()
  sync_vals = s{1,1/3,1/2,1/6,2}
  cb_pitches = s{404,202,606,101,75}
  sd_pitches = s{404,202,606,101,75}
  rs_squish = s{1,2,3,4,5,6,7,8,9,10}
  
  engine.set_param("bd", "amp", 10) -- there's a limiter on each voice, which allows us to saturate with values outside of the normal parameters

  clock.run(iter)
  clock.run(iter_rs)
  clock.run(iter_rs_params)
  
  clock.run(function() while true do clock.sync(1/3) if math.random(100)<30 then engine.trig("bd") engine.trig("cb") end end end)
end

function iter_rs()
  while true do
    clock.sync(1/5)
    if math.random(100) > 30 then
      engine.trig("rs")
    end
  end
end

function iter_rs_params()
  while true do
    clock.sync(1/5)
    if math.random(100) > 30 then
      engine.set_param("rs", "lpHz", math.random(1000,3000))
      engine.set_param("rs", "hpHz", math.random(1000,3000))
      engine.set_param("rs", "pan", math.random(-80,80)/100)
    end
    if math.random(100) < 30 then
      engine.set_param("rs", "squishPitch", rs_squish())
    end
  end
end

function iter()
  while true do
    clock.sync(sync_vals())
    if math.random(100) > 50 then
      engine.set_param("bd", "bitRate", math.random(8000,24000))
      engine.set_param("cb", "carRel", math.random(10,1000)/1000)
      engine.set_param("cb", "carHz", cb_pitches())
      engine.set_param("cb", "pan", math.random(-100,100)/100)
      engine.set_param("cb", "hpHz", math.random(30,12000))
      engine.set_param("sd", "carHz", math.random(287,300))
    end

    engine.set_param("sd", "pan", math.random(-30,30)/100)
    engine.set_param("sd", "carRel", math.random(1,200)/100)
    engine.set_param("sd", "carHz", sd_pitches() * math.random(-3,3))
    engine.trig("sd")
    if math.random(100)>30 then  
      engine.trig("cb")
    end
  end
end