#!/usr/bin/env python3

import os
import io
import sys
import gzip
import math


class Tagger:

    def __init__(self):
        self.initial_tag_probability = None
        self.final_tag_probablility = None
        self.transition_probability = None
        self.emission_probability = None

    def load_corpus(self, path):
        if not os.path.isdir(path):
            sys.exit("Input path is not a directory")
        tuplesList = []
        tagSet = ['NOUN', 'PRONOUN', 'VERB', 'ADVERB', 'ADJECTIVE', 'CONJUNCTION',
                  'PREPOSITION', 'DETERMINER', 'NUMBER', 'PUNCT', 'X']
        for filename in os.listdir(path):
            filename = os.path.join(path, filename)
            try:
                with gzip.open(filename, 'rt') as reader:
                    lines = reader.read().splitlines()
                    for line in lines:
                        lineTuples = []
                        items = line.split()
                        if(len(items) > 0):
                            for item in items:
                                [token, tag] = item.split('/')
                                tag = tag if tag in tagSet else 'X'
                                lineTuples.append((token.lower(), tag))
                            tuplesList.append(lineTuples)
            except IOError:
                sys.exit("Cannot read file")
        return tuplesList

    def initialize_probabilities(self, sentences):
        if type(sentences) != list:
            sys.exit("Incorrect input to method")

        # initialization
        startWithTagCounts = {}
        tagTransitionCounts = {}
        emissionCounts = {}
        endWithTagCounts = {}
        tagSet = ['NOUN', 'PRONOUN', 'VERB', 'ADVERB', 'ADJECTIVE', 'CONJUNCTION',
                  'PREPOSITION', 'DETERMINER', 'NUMBER', 'PUNCT', 'X']
        dictionary = set()
        for tag in tagSet:
            startWithTagCounts[tag] = 0
            endWithTagCounts[tag] = 0
            tagTransitionCounts[tag] = {}
            emissionCounts[tag] = {}
            for nextTag in tagSet:
                tagTransitionCounts[tag][nextTag] = 0
            for line in sentences:
                for item in line:
                    token = item[0]
                    emissionCounts[tag][token] = 0
                    dictionary.add(token)

        # counting the tags
        for line in sentences:
            startTag = line[0][1]
            endTag = line[-1][1]
            startWithTagCounts[startTag] += 1
            endWithTagCounts[endTag] += 1

            prevTag = 'START'
            for item in line:
                curTag = item[1]

                token = item[0]

                emissionCounts[curTag][token] += 1

                if prevTag != 'START':
                    tagTransitionCounts[prevTag][curTag] += 1
                prevTag = curTag

        # computing probabilties

        startWithTagProbs = {}
        endWithTagProbs = {}
        tagTransitionProbs = {}
        emissionProbs = {}

        for tag in tagSet:
            startWithTagProbs[tag] = (
                startWithTagCounts[tag] + 1) / (sum(startWithTagCounts.values()) + len(tagSet))
            endWithTagProbs[tag] = (
                endWithTagCounts[tag] + 1) / (sum(endWithTagCounts.values()) + len(tagSet))

            tagTransitionProbs[tag] = {}
            emissionProbs[tag] = {}
            for nextTag in tagSet:
                tagTransitionProbs[tag][nextTag] = (
                    tagTransitionCounts[tag][nextTag]+1)/(sum(tagTransitionCounts[tag].values())+len(tagSet))
                # tagTransitionProbs[t1][t2] = P(t2 | t1)
            for token in dictionary:
                emissionProbs[tag][token] = (
                    emissionCounts[tag][token]+1) / (sum(emissionCounts[tag].values())+len(dictionary))
                # emissionProbs[tag][token] = P(token | tag)

        self.initial_tag_probability = startWithTagProbs
        self.final_tag_probablility = endWithTagProbs
        self.transition_probability = tagTransitionProbs
        self.emission_probability = emissionProbs
        return

    def viterbi_decode(self, sentence):
        if type(sentence) != str:
            sys.exit("Incorrect input to method")

        (startProbs, endProbs, a, b) = (self.initial_tag_probability, self.final_tag_probablility,
                                        self.transition_probability, self.emission_probability)
        tagSet = set(b.keys())
        tokens = sentence.lower().split()
        viterbi = {}
        backPointer = {}
        # initialization
        for idx in range(0, len(tokens)):
            viterbi[idx] = {}
            backPointer[idx] = {}
        for tag in tagSet:
            viterbi[0][tag] = startProbs[tag] * b[tag][tokens[0]]
            backPointer[0][tag] = 'START'

        # recursion
        for idx in range(1, len(tokens)):
            token = tokens[idx]
            for curTag in tagSet:
                m = -1
                for prevTag in tagSet:
                    n = viterbi[idx-1][prevTag] * \
                        a[prevTag][curTag] * b[curTag][token]
                    if n > m:
                        m = n
                        argMax = prevTag
                viterbi[idx][curTag] = m
                backPointer[idx][curTag] = argMax

        # termination
        m = -1
        for lastTag in tagSet:
            n = viterbi[len(tokens)-1][lastTag]*endProbs[lastTag]
            if n > m:
                m = n
                backPointer['end'] = lastTag

        posSequence = [backPointer['end']]
        for position in range(len(tokens)-1, 0, -1):
            nextPos = []
            curTag = posSequence[0]
            nextPos.append(backPointer[position][curTag])
            nextPos.extend(posSequence)
            posSequence = nextPos
        print(posSequence)
        return
