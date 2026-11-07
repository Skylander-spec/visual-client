// IDs match Begleiter.NAMEN and MiniMe.HUT_NAMEN; never reorder saved IDs.
export const companions = ['Mini-Me', 'Kätzchen', 'Welpe', 'Füchschen', 'Häschen', 'Vogel', 'Küken', 'Kristalldrache', 'Wolkengeist', 'Panda', 'Roter Panda', 'Pinguin', 'Mochi', 'Waldgeist', 'Hummel', 'Axolotl', 'Schildkröte', 'Enderling']
export const hats = ['Keiner', 'Gold', 'Schildkröte', 'Kürbis', 'Kuchen', 'Blume', 'Drache', 'Mütze', 'Katzenohren', 'Hasenohren', 'Rote Mütze']
export const clientDefaults = { miniMe: false, miniMePos: 3, miniMeStand: 1, miniMeArt: 0, miniMeSize: 35, miniMeHut: 0, miniMeFlight: 0, miniMeScarf: 0, miniMeShoes: 0, miniMeSitzt: true, miniMeFluegel: false, zoom: true, customTitle: true, fps: false, keystrokes: false }
export type ClientPreferences = typeof clientDefaults
export const preferenceRanges: Partial<Record<keyof ClientPreferences, [number, number]>> = { miniMePos:[0,3], miniMeStand:[1,1], miniMeArt:[0,17], miniMeSize:[15,70], miniMeHut:[0,10], miniMeFlight:[0,2], miniMeScarf:[0,3], miniMeShoes:[0,3] }
export function validPreference(key: keyof ClientPreferences, value: unknown): boolean {
 const range = preferenceRanges[key]
 return range ? typeof value === 'number' && Number.isInteger(value) && value >= range[0] && value <= range[1] : typeof value === 'boolean'
}
