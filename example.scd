//// Sound Spatialization Model.
//© 2018 Lula Romero
//Dimensions for the Sporthalle Donaueschinger
//Documentation May 2021
//Speakers arrengament (from public) "2 pairs stereo"
//four speakers
//1 Speaker Front Left
//2 Speaker Front Right
//3 Speaker Rear Left
//4 Speaker Rear Right

//BUFFERS Parallax C
(///PATTERN 1
~instrument1_audio = Buffer.read(s,"/home/Documents/audio1.wav"); //adjust path for new audio file
~instrument2_audio = Buffer.read(s,"/home/Documents/audio2.wav"); //adjust path to new audio file
)

//Define ENCODER Matrix
~encoder = FoaEncoderMatrix.newOmni;

//CHOOSE 1 DECODER
// binaural
~decoder = FoaDecoderKernel.newSpherical                   // synthetic binaural (kernel)
//4 speakers
~decoder = FoaDecoderMatrix.newQuad(k: 'dual')             // psycho optimised quad !!
~decoder = FoaDecoderMatrix.newQuad(pi/6, 'dual')          // psycho optimised narrow
//inspect
~decoder.kind;

(
//render Decoder
~renderDecode = { arg in, decoder;
    var kind;
    var fl, bl, br, fr;

    kind = decoder.kind;

    case
        { decoder.numChannels == 2 }
            {
                // decode to stereo (or binaural)
                FoaDecode.ar(in, decoder)
            }
        { kind == 'quad' }
            {
                // decode (to quad)
                #fl, bl, br, fr = FoaDecode.ar(in, decoder);

                // reorder output to match speaker arrangement
                [fl, fr, bl, br]
            };
};

SynthDef("renderDecoder", {arg in;
	Out.ar(0,~renderDecode.value(In.ar(in,4), ~decoder));
}).send;

//synthDef
SynthDef ("model", {arg buffer, azim, angle, distan, gain, out, ampBus, end = 0;

    var sig, fadeout, amp;                            // audio signal / angle control / gain
	amp = In.kr(ampBus).lag(0.2);
	// display encoder & decoder
	"Ambisonic encoding via % encoder".format(~encoder.kind).postln;
	"Ambisonic decoding via % decoder".format(~decoder.kind).postln;
    // ------------------------------------------------------------
    // test sig
    sig = PlayBuf.ar(buffer.numChannels, buffer, loop:0);   // soundfile

	//calculations
	sig = DelayC.ar(sig, 0.2, (distan.lag(0.5)/700)*0.02); //20 miliseconds per 700 cm
	sig = FreeVerb.ar(sig, distan.linlin(200, 4980, 0.1, 0.6), 0.9);//min dist 200 cm maxima distance 4980 cm

	// encode
    sig = FoaEncode.ar(sig, ~encoder);

    // ------------------------------------------------------------
    // transform
	sig = FoaTransform.ar(sig, 'zoom', angle.lag(0.5), azim.lag(0.5), 0, gain.lag(0.5));
	fadeout = EnvGen.kr(Env.new([1,1,0],[0.1,0.4],[1,-2]),end, doneAction:2);

	// ------------------------------------------------------------
    // decode (via ~renderDecode)
	Out.ar(out, sig*amp*fadeout);

}).send;
)
s.meter;


//////////////////////////
//TEST

s.meter;

~test = DifOrchestra(Point(500,100));

~test.addInstr("instr1", Point(2100,3400),~instrument1_audio.bufnum);

//To add new instruments
(
~test.addInstr("instr2", Point(2800,3200),~instrument2_audio.bufnum);
~test.displayGUI;
)

// to delete instruments
(
~test.deleteInstr("instr1");
~test.displayGUI;
)


