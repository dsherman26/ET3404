/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package m6809;

/**
 *
 * @author daves
 */
public class MemoryModule {
    final static int MEMSIZE = 65536;
    final static int ADDRESSMASK = 0xFFFF;
    private final int memArray[];
    MemRegion ROM;
    MemRegion RAM;
    MemRegion DISPLAY;
    MemRegion KEYPAD;
    final static int RAMSTART = 0;
    final int RAMSIZE = 512;
    final static int ROMSTART = 0xF800;
    final static int ROMSIZE = 2048;
    final static int DISPLAYSTART = 0xC110;
    final static int KEYPADSTART = 0xC003;
    final int DISPLAYSIZE = 0x60;
    final int KEYPADSIZE = 8;
    final static int KEYPADDEBOUNCE = 32;
    final static int DEBUGADDRESS = 0;
    int debug;
    
    private final int KeypadCounter[];
    
    final static int ET3404ROM [] = {
        0x4F,0x1F,0x8B,0x10,0xCE,0x00,0xF2,0x17,0x02,0x2C,0x4E,0x67,0x3E,0x00,0x3E,0xE7,
        0x8E,0x00,0xBA,0x9F,0xE8,0x86,0xD0,0xA7,0x84,0x6F,0x03,0x1A,0xD0,0x86,0xFF,0xC6,
        0x08,0x34,0x02,0x5A,0x26,0xFB,0x10,0xCE,0x00,0xDE,0x97,0xE2,0xCE,0xF8,0x2A,0x34,
        0x40,0x17,0x02,0xA9,0x0D,0xE2,0x27,0x08,0x81,0x0F,0x27,0xF5,0x81,0x0B,0x27,0xF1,
        0x48,0xCE,0xFF,0x69,0x33,0xC6,0x96,0xE2,0x6E,0xD4,0x8E,0x00,0xEA,0xCE,0xFF,0xFF,
        0xC6,0x04,0x11,0xA3,0x84,0x27,0x10,0x30,0x02,0x5A,0x26,0xF6,0x17,0x01,0xD7,0x00,
        0x47,0x3E,0x0E,0x0E,0xA0,0x4C,0x39,0x9F,0xE2,0x8D,0x3A,0x1F,0x85,0x8D,0x25,0xEE,
        0x84,0x10,0x8E,0x00,0xF2,0x11,0xA3,0xA3,0x26,0x0C,0x10,0x9C,0xE2,0x27,0x07,0xCE,
        0xFF,0xFF,0xEF,0xA4,0xEE,0x84,0x10,0x8C,0x00,0xEA,0x26,0xE9,0x4C,0x39,0x9F,0xE2,
        0x8D,0x13,0x3D,0x9D,0x8D,0x73,0x9E,0xE2,0xC6,0x02,0x16,0x00,0xDF,0x9F,0xE2,0x8D,
        0x04,0x77,0xBD,0x20,0xEF,0x8E,0xC1,0x2F,0x16,0x02,0xA2,0x9E,0xE8,0x30,0x0A,0x8D,
        0xDD,0x8D,0x56,0x4F,0xC6,0x06,0x17,0x02,0x7B,0x5A,0x26,0xFA,0x8D,0x4B,0xDE,0xE8,
        0xE6,0xD8,0x0A,0x5C,0x6C,0xD8,0x0A,0xE1,0xD8,0x0A,0x26,0x19,0x6A,0xD8,0x0A,0x10,
        0xAE,0x4A,0xC6,0x06,0x8E,0x00,0xEA,0x10,0xAC,0x85,0x27,0x06,0x5A,0x5A,0x2A,0xF7,
        0x20,0x03,0x17,0x02,0x86,0xC6,0x06,0x8E,0x00,0xEA,0x10,0x8E,0x00,0xE8,0xA6,0x95,
        0xA7,0xA2,0x86,0x3F,0xA7,0x95,0x5A,0x5A,0x2A,0xF4,0x8E,0xF9,0x17,0x9F,0xFE,0x10,
        0xDE,0xE8,0xA6,0xE4,0x8A,0x80,0xA7,0xE4,0x3B,0xCE,0xC1,0x6F,0xDF,0xDE,0x39,0x8E,
        0x00,0xE2,0x8D,0x89,0x9E,0xE2,0x39,0x5F,0x1F,0x9B,0x10,0xDF,0xE8,0xEE,0x6A,0x33,
        0x5F,0xEF,0x6A,0x8E,0x00,0xF2,0x10,0x8E,0x00,0xE8,0xA6,0xA2,0xA7,0x93,0x11,0xA3,
        0x84,0x26,0x01,0x5C,0x10,0x8C,0x00,0xE4,0x26,0xF0,0x1F,0x43,0x10,0xCE,0x00,0xDC,
        0x5D,0x10,0x27,0xFF,0x9D,0xAE,0x4A,0x8D,0xC0,0x9F,0xE2,0x8E,0x00,0xE2,0xC6,0x02,
        0x8D,0x03,0xAE,0x84,0x5A,0x16,0x00,0xCD,0x8D,0xB5,0x8D,0xEB,0x8D,0x0E,0x30,0x01,
        0x20,0xF8,0x8D,0xAB,0x30,0x1F,0x30,0x02,0x30,0x1F,0x20,0xDB,0x5D,0x27,0x0C,0x34,
        0x02,0x8D,0x26,0x8D,0x07,0x35,0x02,0x8D,0x20,0x20,0xDA,0x39,0x34,0x04,0x1F,0x13,
        0x86,0x08,0x58,0x17,0x01,0xAE,0x5A,0x26,0xFA,0xE6,0xE4,0x8D,0x0C,0x17,0x01,0x64,
        0xA7,0xC0,0x5A,0x26,0xF8,0x35,0x04,0x4F,0x39,0x34,0x04,0x96,0xDF,0x8B,0x20,0x5A,
        0x26,0xFB,0x97,0xDF,0x35,0x04,0x39,0xC6,0xAA,0xD1,0xE2,0x26,0x0B,0xC6,0xFF,0xD7,
        0xE2,0x17,0x00,0x81,0x30,0xBB,0x20,0x5C,0xD7,0xE2,0x8D,0x79,0x30,0x95,0x20,0x56,
        0x8D,0x73,0x3D,0xE7,0x17,0x01,0x6D,0x17,0x01,0x6A,0x4C,0x97,0xE2,0x20,0x4A,0x17,
        0x00,0x63,0x7D,0x0D,0x0D,0x88,0x4F,0x17,0x01,0x5A,0x17,0x01,0x57,0x17,0x00,0xFD,
        0x97,0xE2,0x81,0x0A,0x27,0x1B,0x81,0x0B,0x27,0x0E,0x81,0x0D,0x26,0xEF,0x17,0x00,
        0x44,0xBD,0x17,0x01,0x3F,0x5C,0x20,0x22,0x17,0x00,0x3A,0x7D,0x0D,0x0D,0x9F,0x20,
        0x18,0x17,0x00,0x31,0x7D,0x0D,0x0D,0xFD,0x20,0x10,0x8D,0x29,0x67,0x8D,0x4C,0x4C,
        0x97,0xE2,0x4C,0x4C,0x4C,0x4C,0x4C,0x4C,0x5C,0x4C,0x4C,0x5C,0x9E,0xE8,0x30,0x86,
        0x8D,0x03,0x96,0xE2,0x39,0x34,0x14,0xA6,0x84,0x17,0x00,0xE9,0x30,0x01,0x5A,0x26,
        0xF6,0x4F,0x35,0x14,0x39,0x5F,0x8E,0xC1,0x6F,0x16,0x01,0x11,0x17,0xFE,0xCA,0x9E,
        0xE8,0xC6,0xA5,0xD1,0xE2,0x26,0x1E,0x86,0x11,0xC6,0x80,0xE5,0x84,0x26,0x02,0x84,
        0x0F,0x54,0xE5,0x84,0x26,0x01,0x4A,0x17,0x00,0xBB,0xC6,0x04,0x4F,0x17,0x00,0xD4,
        0x5A,0x26,0xF9,0x4C,0x39,0x17,0xFE,0xA1,0x9E,0xE8,0xC6,0x20,0x4F,0xE5,0x84,0x27,
        0x01,0x4C,0x17,0x00,0xAA,0x56,0x26,0xF4,0x86,0xA5,0x39,0xC6,0xFF,0xD7,0xE2,0x8D,
        0xB4,0x3E,0xE7,0x20,0x8D,0xC6,0x55,0xD1,0xE2,0x27,0xF0,0xD7,0xE2,0x8D,0xA6,0x5B,
        0xE7,0xCC,0x00,0x0C,0xD3,0xE8,0x8D,0x7D,0x1F,0x98,0x5F,0x8D,0x78,0x96,0xE2,0x39,
        0x34,0x04,0xF6,0xC0,0x03,0xB6,0xC0,0x06,0x48,0x48,0x48,0x59,0x48,0x59,0x48,0x59,
        0x34,0x02,0xB6,0xC0,0x05,0x84,0x1F,0xAB,0xE4,0x43,0x53,0x10,0x8E,0xFF,0x58,0xE7,
        0xE4,0xA1,0xE4,0x27,0x12,0x24,0x06,0x1E,0x98,0x10,0x8E,0xFF,0x60,0x5D,0x26,0x07,
        0x31,0x21,0x48,0x22,0xFB,0x27,0x01,0x4F,0x35,0x06,0xA6,0xA4,0x39,0x34,0x04,0xC6,
        0x20,0x8D,0xBD,0x25,0xFA,0x5A,0x26,0xF9,0xC6,0x20,0x8D,0xB4,0x24,0xFA,0x5A,0x26,
        0xF9,0x35,0x04,0x39,0x8D,0xE7,0x8D,0x27,0x48,0x48,0x48,0x48,0x34,0x04,0x1F,0x89,
        0x8D,0xDB,0x8D,0x1B,0x34,0x04,0xAB,0xE4,0x35,0x04,0x35,0x04,0x34,0x02,0x8D,0x90,
        0x25,0xFC,0x35,0x02,0x39,0x34,0x02,0x44,0x44,0x44,0x44,0x8D,0x02,0x35,0x02,0x34,
        0x02,0x84,0x0F,0x1F,0x12,0x8E,0xFF,0x27,0x30,0x01,0x4A,0x2A,0xFB,0xA6,0x84,0x8D,
        0x05,0x35,0x02,0x39,0x1F,0x12,0x9E,0xDE,0x34,0x04,0x49,0x49,0xC6,0x10,0x49,0xA7,
        0x84,0x30,0x1F,0x5A,0x26,0xF8,0x9F,0xDE,0x1F,0x21,0x35,0x04,0x39,0x9F,0xDE,0xAE,
        0xE4,0x32,0x62,0xA6,0x80,0x8D,0xDD,0x4D,0x2A,0xF9,0x4F,0x6E,0x84,0x8D,0x0C,0x9E,
        0xE8,0xAE,0x0A,0x16,0xFD,0xE1,0x32,0x62,0x16,0xFD,0x7A,0xDE,0xE8,0xAE,0x4A,0xA6,
        0x84,0xC6,0x21,0x10,0x8E,0xFF,0x38,0xA1,0xA0,0x27,0x6F,0x5A,0x26,0xF9,0x5C,0x81,
        0x13,0x27,0xE3,0x81,0x1D,0x27,0x60,0x5C,0x81,0x3C,0x27,0xDA,0x5C,0x81,0x16,0x27,
        0x56,0x81,0x17,0x27,0x52,0x84,0xBF,0x81,0x83,0x27,0x4C,0x84,0xBD,0x81,0x8C,0x27,
        0x46,0xA6,0x84,0x1C,0xFE,0x5A,0x80,0x10,0x25,0x3D,0x80,0x02,0x25,0x40,0x5A,0x80,
        0x08,0x25,0x34,0x5C,0x80,0x16,0x25,0x2F,0x80,0x04,0x25,0x2F,0x80,0x04,0x25,0x27,
        0x5A,0x80,0x28,0x25,0x22,0x5C,0x80,0x10,0x25,0x21,0x5C,0x80,0x10,0x25,0x18,0x5A,
        0x80,0x20,0x25,0x13,0x80,0x10,0x25,0x13,0x5C,0x80,0x10,0x25,0x0A,0x5A,0x80,0x20,
        0x25,0x05,0x80,0x10,0x25,0x05,0x5C,0x16,0x00,0xBA,0x39,0x16,0x00,0x83,0x4C,0x27,
        0x54,0xA6,0x01,0xC6,0x04,0x1C,0xFE,0x80,0x21,0x25,0xEF,0x80,0x0F,0x25,0xE8,0xA6,
        0x01,0x81,0x83,0x27,0xE2,0x81,0x8C,0x27,0xDE,0x81,0x8E,0x27,0xDA,0x81,0xB3,0x27,
        0xD6,0x81,0xBC,0x27,0xD2,0x84,0xBE,0x81,0xBE,0x27,0xCC,0xA6,0x01,0x81,0xCE,0x27,
        0xC6,0x5A,0x84,0xBE,0x81,0xAE,0x27,0xBF,0x81,0x9E,0x27,0xBB,0xA6,0x01,0x81,0x93,
        0x27,0xB5,0x81,0xA3,0x27,0xB1,0x81,0x9C,0x27,0xAD,0x81,0xAC,0x27,0xA9,0x5A,0x81,
        0x3F,0x27,0xA4,0x20,0xA5,0xC6,0x02,0xA6,0x01,0x81,0x3F,0x27,0x9A,0x5C,0x81,0x93,
        0x27,0x1B,0x81,0xA3,0x27,0x95,0x81,0x9C,0x27,0x13,0x81,0xAC,0x27,0x8D,0x5C,0x81,
        0x83,0x27,0x0A,0x81,0x8C,0x27,0x06,0x81,0xB3,0x27,0x02,0x81,0xBC,0x27,0x35,0x20,
        0xD2,0x1F,0x98,0x4A,0xA6,0x86,0x2A,0x2C,0x84,0x1F,0x81,0x10,0x27,0x14,0x81,0x12,
        0x27,0x10,0x81,0x0F,0x27,0x0C,0x84,0x0F,0x81,0x07,0x27,0x06,0x81,0x0A,0x27,0x02,
        0x81,0x0E,0x10,0x27,0xFF,0x54,0x85,0x08,0x27,0x0A,0x81,0x0B,0x27,0x06,0x5C,0x85,
        0x01,0x27,0x01,0x5C,0x20,0x6A,0xA6,0x01,0x5F,0x44,0x24,0x01,0x5C,0x44,0x24,0x01,
        0x5C,0x44,0x24,0x01,0x5C,0x44,0x24,0x01,0x5C,0x44,0x24,0x02,0xCB,0x02,0x44,0x24,
        0x02,0xCB,0x02,0x44,0x24,0x02,0xCB,0x02,0x44,0x39,0x10,0xAE,0x4C,0x17,0x02,0x11,
        0x39,0xE6,0x01,0x1D,0x31,0x02,0x10,0x9F,0xE2,0xD3,0xE2,0x1F,0x02,0x20,0xEE,0x31,
        0x4F,0x10,0xAE,0x27,0xA6,0x4C,0x2B,0xE5,0x10,0xAE,0x4D,0x20,0xE0,0xEC,0x01,0x31,
        0x03,0x20,0xE3,0xEC,0x02,0x31,0x04,0x20,0xDD,0x8D,0xAB,0x24,0x05,0xCB,0x0C,0x10,
        0xAE,0xC5,0x20,0xC9,0x8D,0xA0,0x24,0x06,0x10,0xAE,0x48,0x10,0xAE,0xA5,0x20,0xBD,
        0xD7,0xE0,0x10,0x8E,0xFF,0xFF,0xA6,0x84,0x81,0x16,0x27,0xD1,0x81,0x3B,0x27,0xBF,
        0x81,0x39,0x27,0xA6,0x81,0x8D,0x27,0xA9,0x81,0x17,0x27,0xC1,0x81,0x3F,0x10,0x27,
        0x01,0x7D,0x81,0x0E,0x27,0x55,0x81,0x6E,0x27,0x5B,0x81,0x7E,0x27,0x48,0x81,0x9D,
        0x27,0x49,0x81,0xBD,0x27,0x40,0x81,0xAD,0x27,0x4B,0x81,0x35,0x27,0xAB,0x81,0x37,
        0x27,0xB2,0x84,0xFE,0x81,0x1E,0x10,0x27,0x01,0x00,0x84,0xF0,0x81,0x20,0x10,0x27,
        0xFF,0x6F,0xEC,0x84,0x81,0x10,0x26,0x10,0xC1,0x3F,0x10,0x27,0x01,0x4A,0xC4,0xF0,
        0xC1,0x20,0x10,0x27,0xFF,0x7D,0x20,0x0A,0x81,0x11,0x26,0x06,0xC1,0x3F,0x10,0x27,
        0x01,0x3D,0x17,0x01,0x5C,0x39,0x10,0xAE,0x01,0x20,0x06,0xA6,0x43,0xE6,0x01,0x1F,
        0x02,0x17,0x01,0x4D,0x39,0xA6,0x04,0x4A,0x6A,0x04,0xA1,0x04,0x26,0x06,0x6C,0x04,
        0x8D,0x66,0x20,0xED,0xA6,0x82,0x10,0xAE,0x01,0x10,0xAF,0x84,0x10,0xAE,0x03,0x10,
        0xAF,0x02,0xE6,0x01,0x34,0x06,0x10,0xAE,0x4A,0x31,0x3F,0x10,0xAF,0x4A,0xA6,0x01,
        0x2A,0x0C,0x84,0x1F,0x81,0x1F,0x27,0x06,0xA6,0x01,0x84,0xEF,0xA7,0x01,0x8D,0x38,
        0x34,0x20,0x10,0xAE,0x02,0x10,0xAF,0x03,0x10,0xAE,0x84,0x10,0xAF,0x01,0x6C,0x4B,
        0x26,0x02,0x6C,0x4A,0x35,0x20,0x35,0x06,0xA7,0x80,0xE7,0x01,0xC4,0x0F,0x5C,0xC1,
        0x0D,0x2B,0x02,0x31,0x21,0xA6,0x01,0x2A,0x0D,0x84,0x1F,0x81,0x1F,0x27,0x07,0x85,
        0x10,0x27,0x03,0x10,0xAE,0xA4,0x20,0x89,0x10,0xAE,0x84,0x10,0x9F,0xE6,0x86,0x30,
        0xA7,0x84,0xA6,0xC4,0x97,0xE1,0x10,0xAE,0x44,0x10,0x9F,0xE4,0xA6,0x01,0x84,0x60,
        0x27,0x11,0x10,0xAE,0x46,0x80,0x20,0x27,0x0A,0x10,0xAE,0x48,0x80,0x20,0x27,0x03,
        0x10,0x9E,0xE2,0x10,0xAF,0x44,0xE6,0x01,0xC4,0x9F,0xE7,0x01,0x17,0x00,0xA2,0xD6,
        0xE0,0x30,0x1F,0x5A,0x26,0xFB,0xAF,0x4A,0x10,0x9E,0xE6,0x10,0xAF,0x84,0x10,0xAE,
        0x44,0xDC,0xE4,0xED,0x44,0x96,0xE1,0xA7,0xC4,0x39,0xE6,0x01,0x2A,0x10,0xC5,0x08,
        0x27,0x4C,0xC1,0xBF,0x2E,0x48,0xC4,0x0F,0xC1,0x0C,0x2C,0x42,0x20,0x3E,0xC5,0x08,
        0x26,0x3C,0xC1,0x5F,0x2E,0x38,0xC4,0x0F,0xC1,0x06,0x2A,0x32,0xC1,0x05,0x27,0x0E,
        0xE6,0x01,0xC4,0xF0,0xC1,0x50,0x26,0x24,0xE6,0x01,0x5C,0x58,0x20,0x07,0xE6,0x01,
        0xCB,0x10,0x54,0x54,0x54,0xC4,0x0E,0xC1,0x02,0x27,0x0D,0xC1,0x0C,0x27,0x08,0xC1,
        0x0A,0x26,0x06,0x31,0x4C,0x20,0x05,0x5A,0x5A,0x10,0xAE,0xC5,0x8D,0x33,0x39,0x8E,
        0x00,0xFA,0x86,0xD0,0x97,0xE1,0x20,0x0C,0x8E,0x00,0xF4,0x86,0x80,0x20,0xF5,0x8E,
        0x00,0xF2,0x20,0xF7,0xD6,0xE0,0x4F,0xE3,0x4A,0xED,0x4A,0x86,0x0C,0xE6,0x4B,0xE7,
        0xC2,0x4A,0x26,0xF9,0xEC,0x84,0xED,0x4A,0xDF,0xE8,0x96,0xE1,0xAA,0xC4,0xA7,0xC4,
        0x39,0xD6,0xE0,0x3A,0xA6,0x84,0xE6,0xA4,0x34,0x36,0x86,0x3F,0xA7,0x84,0xA7,0xA4,
        0x8E,0xFE,0xF9,0x10,0xDF,0xE2,0x16,0xFA,0x04,0x5F,0x1F,0x9B,0x1F,0x43,0xDF,0xE8,
        0x10,0xDE,0xE2,0xAE,0x4A,0x30,0x1F,0xAF,0x4A,0x35,0x36,0xE7,0xA4,0xA7,0x84,0x39,
        0x6E,0x9F,0x00,0xF2,0x6E,0x9F,0x00,0xF4,0x6E,0x9F,0x00,0xF6,0x6E,0x9F,0x00,0xF8,
        0x6E,0x9F,0x00,0xFE,0x6E,0x9F,0x00,0xFC,0x7E,0x30,0x6D,0x79,0x33,0x5B,0x5F,0x70,
        0x7F,0x7B,0x77,0x1F,0x4E,0x3D,0x4F,0x47,0x01,0x02,0x05,0x0B,0x14,0x15,0x18,0x1B,
        0x38,0x3E,0x41,0x42,0x45,0x4B,0x4E,0x51,0x52,0x55,0x5B,0x5E,0x61,0x62,0x65,0x6B,
        0x71,0x72,0x75,0x7B,0x87,0x8F,0xC7,0xCD,0xCF,0x07,0x0A,0x0D,0x02,0x05,0x08,0x0B,
        0x0E,0x03,0x06,0x09,0x0C,0x0F,0x00,0x01,0x04,0xF8,0x8D,0xF9,0xCF,0xF9,0xC0,0xFA,
        0x0A,0xF9,0xA7,0xFA,0x3C,0xFA,0x85,0xF8,0xB1,0xFB,0x5D,0xF8,0x4A,0xF9,0x58,0xF9,
        0x68,0xF9,0x6C,0xF8,0xAB,0xF9,0x62,0xF9,0x66,0x6F,0xC4,0x6F,0x41,0xA6,0x01,0xE6,
        0x21,0x3D,0xED,0x42,0xA6,0x84,0xE6,0x21,0x3D,0xE3,0x41,0xED,0x41,0x24,0x02,0x6C,
        0xC4,0xA6,0x01,0xE6,0xA4,0x3D,0xE3,0x41,0xED,0x41,0x24,0x02,0x6C,0xC4,0xA6,0x84,
        0xE6,0xA4,0x3D,0xE3,0xC4,0xED,0xC4,0x39,0x00,0x80,0x00,0xFF,0x00,0xFF,0x80,0x00,
        0x00,0x00,0x00,0xFF,0x00,0xFF,0x00,0x00,0x00,0x00,0x00,0xFF,0x00,0xFF,0x00,0x20,
        0x00,0x20,0x00,0xFF,0x00,0xFF,0x00,0x00,0x40,0x00,0x00,0xFF,0x00,0xFF,0x00,0x00,
        0x00,0x00,0x00,0xFF,0x00,0xFF,0x00,0x00,0x00,0x00,0x00,0xFF,0x00,0xFF,0x00,0x00,
        0x00,0x04,0xFF,0x10,0xFF,0x14,0xFF,0x18,0xFF,0x1C,0xFF,0x20,0xFF,0x24,0xF8,0x00
    };
    
    public void MemWrite(int iAddress, int iValue)
    {
        iAddress &= ADDRESSMASK;
        if(iAddress == DEBUGADDRESS)
            debug = 1;
        memArray[iAddress] = (iValue & 0xFF);
    }

/*
**      MemRead - Accessor for reading RAM/ROM
*/
    public int MemRead(int iAddress)
    {  
        iAddress &= ADDRESSMASK;
        if ((iAddress >= RAM.memstart && iAddress < (RAM.memstart + RAM.memsize)) ||
                (iAddress >= DISPLAY.memstart && iAddress < (DISPLAY.memstart + DISPLAY.memsize)))
        {
            return (memArray[iAddress]);
        }
        else if (iAddress >= KEYPAD.memstart && iAddress < (KEYPAD.memstart + KEYPAD.memsize))
        {
            //Since a real ET-3400A uses a multiplexed keypad, translate virtual
            //keypresses into hardware bits.
            return(KeypadRead(iAddress));
        }
        else if (iAddress >= ROM.memstart && iAddress < (ROM.memstart + ROM.memsize))
        {
            return (ET3404ROM[iAddress - ROM.memstart]);
        }
        else
            return (0);
    }
    
    public MemoryModule()
    {
        int iCounter;
        memArray = new int[MEMSIZE];
        KeypadCounter = new int[16];
        for(iCounter = 0; iCounter < MEMSIZE; iCounter++)
            memArray[iCounter] = 0;
        ROM = new MemRegion(ROMSTART, ROMSIZE);
        RAM = new MemRegion(RAMSTART, RAMSIZE);
        DISPLAY = new MemRegion(DISPLAYSTART, DISPLAYSIZE);
        KEYPAD = new MemRegion(KEYPADSTART, KEYPADSIZE);
    }
    
    public int KeypadRead(int address)
    {
        int iValue = 0xFF;
        if((address & 1) == 0) // 0, 1, 4, 7, A, D keys
        {
            if(KeypadCounter[0] > 0) // 0 key
            {
                iValue &= ~(1<<5);
                KeypadCounter[0]--;
            }
            if(KeypadCounter[1] > 0) // 1 key
            {
                iValue &= ~(1<<4);
                KeypadCounter[1]--;
            }
            if(KeypadCounter[4] > 0)
            {
                iValue &= ~(1<<3);
                KeypadCounter[4]--;
            }
            if(KeypadCounter[7] > 0)
            {
                iValue &= ~(1<<2);
                KeypadCounter[7]--;
            }
            if(KeypadCounter[10] > 0)
            {
                iValue &= ~(1<<1);
                KeypadCounter[10]--;
            }
            if(KeypadCounter[13] > 0)
            {
                iValue &= ~(1);
                KeypadCounter[13]--;
            }
        }
        if((address & (1<<1)) == 0) //2, 5, 8, B, E keys
        {
            if(KeypadCounter[2] > 0)
            {
                iValue &= ~(1<<4);
                KeypadCounter[2]--;
            }
            if(KeypadCounter[5] > 0)
            {
                iValue &= ~(1<<3);
                KeypadCounter[5]--;
            }
            if(KeypadCounter[8] > 0)
            {
                iValue &= ~(1<<2);
                KeypadCounter[8]--;
            }
            if(KeypadCounter[11] > 0)
            {
                iValue &= ~(1<<1);
                KeypadCounter[11]--;
            }
            if(KeypadCounter[14] > 0)
            {
                iValue &= ~(1);
                KeypadCounter[14]--;
            }
        }
        if((address & (1<<2)) == 0) // 3, 6, 9, C, F keys
        {
            if(KeypadCounter[3] > 0)
            {
                iValue &= ~(1<<4);
                KeypadCounter[3]--;
            }
            if(KeypadCounter[6] > 0)
            {
                iValue &= ~(1<<3);
                KeypadCounter[6]--;
            }
            if(KeypadCounter[9] > 0)
            {
                iValue &= ~(1<<2);
                KeypadCounter[9]--;
            }
            if(KeypadCounter[12] > 0)
            {
                iValue &= ~(1<<1);
                KeypadCounter[12]--;
            }
            if(KeypadCounter[15] > 0)
            {
                iValue &= ~(1);
                KeypadCounter[15]--;
            }
        }
        return(iValue & 0xFF);
    }
    
    public void KeypadWrite(int key)
    {
        if(KeypadCounter[key] == 0)
        {
            KeypadCounter[key] = KEYPADDEBOUNCE;
        }
    }
}

class MemRegion
{
    int memstart;
    int memsize;
    MemRegion(int start, int size)
    {
       this.memstart = start;
       this.memsize = size;
    }
}