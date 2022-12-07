UPDATE linking_rules
SET jsonb = '[
  {
    "bibField": "100",
    "authorityField": "100",
    "authoritySubfields": ["a", "b", "c", "d", "j", "q"],
    "subfieldModifications": [],
    "validation": {
      "existence": [
        { "t": false }
      ]
    }
  },
  {
    "bibField": "110",
    "authorityField": "110",
    "authoritySubfields": ["a", "b", "c"],
    "subfieldModifications": [],
    "validation": {
      "existence": [
        { "t": false }
      ]
    }
  },
  {
    "bibField": "111",
    "authorityField": "111",
    "authoritySubfields": ["a", "c", "e", "q"],
    "subfieldModifications": [],
    "validation": {
      "existence": [
        { "t": false }
      ]
    }
  },
  {
    "bibField": "130",
    "authorityField": "130",
    "authoritySubfields": ["a", "d", "f", "g", "h", "k", "l", "m", "n", "o", "p", "r", "s", "t"],
    "subfieldModifications": [],
    "validation": {}
  },
  {
    "bibField": "240",
    "authorityField": "100",
    "authoritySubfields": ["f", "g", "h", "k", "l", "m", "n", "o", "p", "r", "s", "t"],
    "subfieldModifications": [
      {
        "source": "t",
        "target": "a"
      }
    ],
    "validation": {
      "existence": [{ "t": true }]
    }
  },
  {
    "bibField": "240",
    "authorityField": "110",
    "authoritySubfields": ["d", "f", "g", "h", "k", "l", "m", "n", "o", "p", "r", "s", "t"],
    "subfieldModifications": [
      {
        "source": "t",
        "target": "a"
      }
    ],
    "validation": {
      "existence": [{ "t": true }]
    }
  },
  {
    "bibField": "240",
    "authorityField": "111",
    "authoritySubfields": ["d", "f", "g", "h", "k", "l", "n", "p", "s", "t"],
    "subfieldModifications": [
      {
        "source": "t",
        "target": "a"
      }
    ],
    "validation": {
      "existence": [{ "t": true }]
    }
  },
  {
    "bibField": "600",
    "authorityField": "100",
    "authoritySubfields": ["a", "b", "c", "d", "g", "j", "q", "f", "h", "k", "l", "m", "n", "o", "p", "r", "s", "t", "v", "x", "y", "z"],
    "subfieldModifications": [],
    "validation": {}
  },
  {
    "bibField": "610",
    "authorityField": "110",
    "authoritySubfields": ["a", "b", "c", "d", "g", "f", "h", "k", "l", "m", "n", "o", "p", "r", "s", "t", "v", "x", "y", "z"],
    "subfieldModifications": [],
    "validation": {}
  },
  {
    "bibField": "611",
    "authorityField": "111",
    "authoritySubfields": ["a", "c", "e", "q", "f", "h", "k", "l", "p", "s", "t", "d", "g", "n", "v", "x", "y", "z"],
    "subfieldModifications": [],
    "validation": {}
  },
  {
    "bibField": "630",
    "authorityField": "130",
    "authoritySubfields": ["a", "d", "f", "g", "h", "k", "l", "m", "n", "o", "p", "r", "s", "t", "v", "x", "y", "z"],
    "subfieldModifications": [],
    "validation": {}
  },
  {
    "bibField": "650",
    "authorityField": "150",
    "authoritySubfields": ["a", "b", "g", "v", "x", "y", "z"],
    "subfieldModifications": [],
    "validation": {}
  },
  {
    "bibField": "651",
    "authorityField": "151",
    "authoritySubfields": ["a", "g", "v", "x", "y", "z"],
    "subfieldModifications": [],
    "validation": {}
  },
  {
    "bibField": "655",
    "authorityField": "155",
    "authoritySubfields": ["a", "v", "x", "y", "z"],
    "subfieldModifications": [],
    "validation": {}
  },
  {
    "bibField": "700",
    "authorityField": "100",
    "authoritySubfields": ["a", "b", "c", "d", "j", "q", "f", "h", "k", "l", "m", "n", "o", "p", "r", "s", "t", "g"],
    "subfieldModifications": [],
    "validation": {}
  },
  {
    "bibField": "710",
    "authorityField": "110",
    "authoritySubfields": ["a", "b", "c", "f", "h", "k", "l", "m", "o", "p", "r", "s", "t", "d", "g", "n"],
    "subfieldModifications": [],
    "validation": {}
  },
  {
    "bibField": "711",
    "authorityField": "111",
    "authoritySubfields": ["a", "c", "e", "q", "f", "h", "k", "l", "p", "s", "t", "d", "g", "n"],
    "subfieldModifications": [],
    "validation": {}
  },
  {
    "bibField": "730",
    "authorityField": "130",
    "authoritySubfields": ["a", "d", "f", "g", "h", "k", "l", "m", "n", "o", "p", "r", "s", "t"],
    "subfieldModifications": [],
    "validation": {}
  },
  {
    "bibField": "800",
    "authorityField": "100",
    "authoritySubfields": ["a", "b", "c", "d", "j", "q", "f", "h", "k", "l", "m", "n", "o", "p", "r", "s", "t", "g"],
    "subfieldModifications": [],
    "validation": {}
  },
  {
    "bibField": "810",
    "authorityField": "110",
    "authoritySubfields": ["a", "b", "c", "f", "h", "k", "l", "m", "o", "p", "r", "s", "t", "d", "g", "n"],
    "subfieldModifications": [],
    "validation": {}
  },
  {
    "bibField": "811",
    "authorityField": "111",
    "authoritySubfields": ["a", "c", "e", "q", "f", "h", "k", "l", "p", "s", "t", "d", "g", "n"],
    "subfieldModifications": [],
    "validation": {}
  },
  {
    "bibField": "830",
    "authorityField": "130",
    "authoritySubfields": ["a", "d", "f", "g", "h", "k", "l", "m", "n", "o", "p", "r", "s", "t"],
    "subfieldModifications": [],
    "validation": {}
  }
]'
WHERE linking_pair_type = 'INSTANCE_AUTHORITY'
