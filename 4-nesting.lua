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
  screen_dirty = true

end

function establish_sequences()
  sequences = {
    [1] = {
      -- let's build some nested tables, to control parameters per-step:
      bd = s{
        {1, modAmp = 0.8, feedAmp = 80, delaySend = 0.4},
        0,
        0,
        {0, modAmp = 0, carRel = 0.2},
        {1, prob = 70},
        0,
        {1, prob = 40},
        {0, reverbSend = 0, delaySend = 0},
        {0, carRel = 0.8, modRel = 0.8, feedAmp = 0},
        {1, prob = 80},
        0,
        {0, modRel = 0.05, reverbSend = 1}
      },
      sd = s{0},
      tm = s{0},
      cp = s{0},
      rs = s{
        { 1, prob = 15, squishPitch = {rand = {1,9}} , pan = {rand = {-0.5,0.5}}},
        { 0, delaySend = {rand = {0,1}}}
      },
      cb = s{0},
      hh = s{
        { 1, prob = 32, amp = {rand = {0.2,0.7}} }
      }
    },

  }
  
  selected_seq = 1
  queued_seq = 1
  
  drum_active = {}
  for k,v in pairs(drums) do
    drum_active[v] = false
  end
    
end

function play_sequence()
  while true do
    clock.sync(1/4)
    for i = 1,#drums do
      if sequences[selected_seq][drums[i]] ~= nil then
        local eval_step = sequences[selected_seq][drums[i]]()
        if type(eval_step) == "table" then
          for k,v in pairs(eval_step) do
            if k ~= 1 and k ~= 0 and k ~= "prob" then
              if type(v) ~= "table" then
                params:set(drums[i].."_"..k,v)
              else
                for modifier,value in pairs(v) do
                  if modifier == "rand" then
                    local send = math.random(util.round(value[1]*100), util.round(value[2])*100)/100
                    params:set(drums[i].."_"..k,send)
                  end
                end
              end
            end
          end
          if eval_step[1] == 1 and eval_step.prob == nil then
            play_drum(drums[i])
          elseif eval_step[1] == 1 and eval_step.prob ~= nil then
            if math.random(0,100) <= eval_step.prob then
              play_drum(drums[i])
            end
          end
        else
          if eval_step == 1 then
            engine.trig(drums[i])
          end
        end
      end
      screen_dirty = true
    end
  end
end

function play_drum(name)
  drum_active[name] = true
  engine.trig(name)
  screen_dirty = true
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
    local current_val = drum_active[focused_drum]
    screen.level(current_val == true and 15 or 3)
    screen.move((18*(i-1))+3,30)
    screen.text(drums[i])
    screen.move((18*(i-1))+6,40)
    screen.text(current_val == true and 1 or 0)
    if current_val then
      drum_active[focused_drum] = false
    end
  end
  screen.level(5)
  screen.move(0,60)
  screen.text("K2: "..(transport_active and "stop" or "start"))
  screen.move(128,60)
  screen.text_right("K3: load seq "..queued_seq)
  screen.update()
end