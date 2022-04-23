engine.name = 'Kildare'
s = require 'sequins'

kildare_setup = include 'lib/kildare'

function init()
  kildare_setup.init()
  sync_vals = s{1,1/3,1/2,1/6,2}
  cb_pitches = s{404,202,606,101,75}
  sd_pitches = s{404,202,606,101,75}
  rs_squish = s{1,2,3,4,5,6,7,8,9,10}

  -- for k,v in pairs(param_seq[selected_seq]) do
  --   local drum_target = k
  --   local param_pool = param_seq[selected_seq][drum_target]
  --   for key,value in pairs(param_pool) do
  --     local param_target = key
  --     local param_value = param_pool[param_target]()
  --     engine.set_param(drum_target, param_target, param_value)
  --   end
  -- end
  
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
      -- engine.trig_rs()
    end
  end
end

function iter_rs_params()
  while true do
    clock.sync(1/5)
    if math.random(100) > 30 then
      engine.set_param("rs", "lpHz", math.random(1000,3000))
      -- engine.rs_LPfreq(math.random(1000,3000))
      engine.set_param("rs", "hpHz", math.random(1000,3000))
      -- engine.rs_HPfreq(math.random(1000,3000))
      engine.set_param("rs", "pan", math.random(-80,80)/100)
      -- engine.rs_pan(math.random(-80,80)/100)
    end
    if math.random(100) < 30 then
      engine.set_param("rs", "squishPitch", rs_squish())
      -- engine.rs_SPTCH(rs_squish())
    end
  end
end

function iter()
  while true do
    clock.sync(sync_vals())
    if math.random(100) > 50 then
      -- engine.sd_brate(math.random(8000,24000))
      engine.set_param("bd", "brate", math.random(8000,24000))
      -- engine.cb_carRel(math.random(10,1000)/1000)
      engine.set_param("cb", "carRel", math.random(10,1000)/1000)
      -- engine.cb_carHz(cb_pitches())
      engine.set_param("cb", "carHz", cb_pitches())
      -- engine.cb_pan(math.random(-100,100)/100)
      engine.set_param("cb", "pan", math.random(-100,100)/100)
      -- engine.cb_HPfreq(math.random(30,12000))
      engine.set_param("cb", "hpHz", math.random(30,12000))
      -- engine.sd_carHz(math.random(287,300))
      engine.set_param("sd", "carHz", math.random(287,300))
    end
    -- engine.sd_pan(math.random(-30,30)/100)
    engine.set_param("sd", "pan", math.random(-30,30)/100)
    -- engine.sd_carRel(math.random(1,200)/100)
    engine.set_param("sd", "carRel", math.random(1,200)/100)
    -- engine.sd_carHz(sd_pitches() * math.random(-3,3))
    engine.set_param("sd", "carHz", sd_pitches() * math.random(-3,3))
    -- engine.trig_sd()
    engine.trig("sd")
    if math.random(100)>30 then
      -- engine.trig_cb()
      engine.trig("cb")
    end
  end
end