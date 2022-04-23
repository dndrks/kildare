engine.name = 'Kildare'
s = require 'sequins'
drums = {"bd","sd","tm","cp","rs","cb","hh"}

kildare_setup = include 'lib/kildare'

function init()
  kildare_setup.init()
  establish_sequences()
  
  pattern_launch_quant = 1/4 -- wait till the 1/16th note to launch a queued pattern
  
  screen_redraw = clock.run(
    function()
      while true do
        clock.sleep(1/15)
        if screen_dirty then
          redraw()
          screen_dirty = false
        end
      end
    end
  )
end

-- sequins parameter scratchpad
-- uncomment and live-execute to infuse modulation into the sequences
-- param_seq[1].bd_pan = s{0.3,0.6,-1,0}
-- param_seq[1].bd_bitRate = s{1000,12000,400,1500}
-- param_seq[selected_seq].bd_bitRate = s{12000}
-- param_seq[selected_seq].bd_carHz = s{30,50,600,300,100}
-- param_seq[selected_seq].sd_carHz = s{300,500,200,2000,4000,17000,400}
-- param_seq[selected_seq].sd_carHz = s{params:get("sd_carHz")}
-- param_seq[4].cb_rampDepth = s{1,0,0.3,0.4,0.8,0,0}

-- sequins sequencer scratchpad
-- uncomment and live-execute to infuse changes into the sequences
-- sequences[selected_seq].bd = s{1,0,1,1}
-- sequences[selected_seq].sd = s{1,1,0,1,0,0}
-- sequences[selected_seq].hh = s{0,1}

function establish_sequences()
  sequences = {
    [1] = {
      bd = s{1,0,0,0,1,0,0,0},
      sd = s{0,1,0,1,1,0,1,0},
      tm = s{1,0,1,0,0,1,0,1},
      cp = s{0,0,0,0,0,0,0,1},
      rs = s{0,0,0,1,0,1,0,0},
      cb = s{0,0,0,0,0,0,0,0},
      hh = s{1,1,0,0,1,1,1,0}
    },
    [2] = {
      bd = s{1,0,0,0,1},
      sd = s{0,1,0,1,0},
      tm = s{1,0,1,0,0},
      cp = s{0,0,0,0,0},
      rs = s{0,0,0,1,0},
      cb = s{0,0,0,0,0},
      hh = s{1,1,0,0,0}
    },
    [3] = {
      bd = s{1,0,0,0,1,0,0,0,1,0,0,1,0,1,0,1},
      sd = s{0,1,0,1,1,0,1,0,0,0,0,0,1,1,1,1},
      tm = s{1,0,1,0,0,0,0,0,1,1,0,0},
      cp = s{0,0,0,0,0,1,0,0,0,0},
      rs = s{0,0,0,1,0,1,0,0,0,0,1},
      cb = s{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
      hh = s{1,1,0,0,0,0,1,0,1},
    },
    [4] = {
      bd = s{1,1,0,0},
      sd = s{0,1,1,0},
      tm = s{0,1,0,0},
      cp = s{0,0,0,1,0,1},
      rs = s{0,0,0,1,0,1},
      cb = s{0,0,1,0,0},
      hh = s{1,1,0}
    },
  }
  
  param_seq = {
    [1] = {
      bd = {
        modAmp = s{1,0.4,0.3,0.5,0},
        modRel = s{0.05,1,0.4,0.8},
        carHz = s{60,90,100,1000,3000},
      },
      sd = {
        modAmp = s{1,0.4,0.3,0.5,0},
        modHz = s{params:get("sd_modHz")},
      },
      hh = {
        pan = s{-1,0,1}
      }
    },
    [2] = {
      bd = {
        modAmp = s{1,0.4,1,0,0},
        modRel = s{0.05,0.3,0.4,0.8,1,10},
        carHz = s{30,700,126},
      },
      sd = {
        modAmp = s{1,0.5,0},
        modHz = s{2770,300,1000,5000,403},
      },
      hh = {
        pan = s{-0.3,0,0.3,1,-1}
      },
    },
    [3] = {
      bd = {
        modAmp = s{1,0.4,0.3,0.5,0},
        modRel = s{0.05,1,0.4,0.8},
        carHz = s{60,90,100,1000,3000},
      },
      hh = {
        bitRate = s{1000,12000,400,1500},
        hpHz = s{2300,20,4000,300}
      },
    },
    [4] = {
      bd = {
        modAmp = s{1,0.4,0.3,0.5,0},
        modRel = s{0.05,1,0.4,0.8},
        carHz = s{60,90,100,1000,3000},
      },
    },
  }
  
  selected_seq = 1
  queued_seq = 2
end

function play_sequence()
  while true do
    clock.sync(1/4)
    for i = 1,#drums do
      if sequences[selected_seq][drums[i]]() == 1 then
        engine.trig(drums[i])
      end
    end
    for k,v in pairs(param_seq[selected_seq]) do
      local drum_target = k
      local param_pool = param_seq[selected_seq][drum_target]
      for key,value in pairs(param_pool) do
        local param_target = key
        local param_value = param_pool[param_target]()
        engine.set_param(drum_target, param_target, param_value)
      end
    end
    screen_dirty = true
  end
end

function clock.transport.start()
  reset_indices()
  transport_start()
end

function clock.transport.stop()
  if _running then
    clock.cancel(_running)
  end
  transport_active = false
  screen_dirty = true
end

function transport_start()
  _running = clock.run(play_sequence)
  transport_active = true
end

function change_sequence(target)
  reset_indices()
  selected_seq = target  
end


function enc(n,d)
  if n == 3 then
    queued_seq = util.clamp(queued_seq + d, 1, #sequences)
    screen_dirty = true
  end
end

function key(n,z)
  if n == 3 and z == 1 then
    clock.run(
      function()
        clock.sync(pattern_launch_quant)
        change_sequence(queued_seq)
      end
    )
    
  elseif n == 2 and z == 1 then
    -- since MIDI and Link offer their own start/stop messages,
    -- we'll only need to manually start if using internal or crow clock sources:
    if params:string("clock_source") == "internal" or params:string("clock_source") == "crow" then
      if transport_active then
        clock.transport.stop()
      else
        clock.transport.start()
      end
    end
  end
end

function reset_indices()
  for i = 1,#drums do
    sequences[selected_seq][drums[i]]:reset()
  end
  screen_dirty = true
end

function redraw()
  screen.clear()
  screen.level(5)
  screen.move(128,10)
  screen.text_right("current seq: "..selected_seq)
  for i = 1,#drums do
    local focused_drum = drums[i]
    local current_ix = sequences[selected_seq][focused_drum].ix
    local current_val = sequences[selected_seq][focused_drum].data[current_ix]
    screen.level(current_val == 1 and 15 or 3)
    screen.move((18*(i-1))+3,30)
    screen.text(drums[i])
    screen.move((18*(i-1))+6,40)
    screen.text(current_val)
  end
  screen.level(5)
  screen.move(0,60)
  screen.text("K2: "..(transport_active and "stop" or "start"))
  screen.move(128,60)
  screen.text_right("K3: load seq "..queued_seq)
  screen.update()
end