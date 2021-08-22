#!/usr/bin/env python3
import sys
import os
from tagger import Tagger


t = Tagger()
sentences = t.load_corpus(os.path.join(sys.path[0], sys.argv[1]))
t.initialize_probabilities(sentences)
t.viterbi_decode('people race tomorrow .')
t.viterbi_decode('the secretariat is expected to race tomorrow .')
t.viterbi_decode('people continue to inquire the reason for the race for outer space .')
