package the.kis.devs.consolerendering

import jline.TerminalFactory
import kotlin.math.*

/**
 * @author _kisman_
 * @since 19:52 of 21.02.2023
 */

fun main() {
    val terminal = TerminalFactory.create()

    val width = terminal.width.toDouble() - 1
    val height = terminal.height.toDouble()

    /**
     * Size of *pixel* of mono 12 font
     */
    val pixelWidth = 11.0
    val pixelHeight = 24.0

    val aspect = width / height

    /**
     * Size of *pixel* of mono 12 font
     */
    val pixelAspect = pixelWidth / pixelHeight

    val gradient = ".:!/r(l1Z4H9W8\$@".toCharArray()
    val gradientSize = gradient.size - 1

    for(t in 0..30000) {
//        val light = norm(vec3(-0.5, 0.5, -1.0))
        val light = norm(vec3(sin(t * 0.001), cos(t * 0.001), -1))
//        val spherePos = vec3(0.0, 3.0, 0.0)

        val matrix = mutableMapOf<Vec2, String>()

        for(i in 0 until width.toInt()) {
            for(j in 0 until height.toInt()) {
                val uv = vec2(i , j) / vec2(width, height) * 2 - 1

                uv.x *= aspect * pixelAspect

//                var ro = vec3(-5, sin(t * 0.001), -1)
                var ro = vec3(-7, 0, 0)
//                val ro = vec3(sin(t * 0.001), 0, cos(t * 0.001))
                var rd = norm(vec3(2, uv.x, uv.y))

                rotateY(ro, 0.5)
                rotateY(rd, 0.5)

                rotateZ(ro, t * 0.001)
                rotateZ(rd, t * 0.001)
//                rotateZ(ro, t * 0.01)
//                rotateZ(rd, t * 0.01)

                var diff = 1.0
                var minIt = 99999.0
                var needColor = false

                fun drawSphere(
                    pos : Vec3
                ) : Triple<Vec3, Double, Double>? {
                    val intersection = sphere(ro - pos, rd, 1.0)

                    return if(intersection.x > 0) {
                        Triple(norm(ro - pos + rd * intersection.x), 1.0, intersection.x)
                    } else {
                        null
                    }
                }

                fun drawPlane() : Triple<Vec3, Double, Double>? {
                    val intersection = plane(ro, rd, vec3(0, 0, -1), 1.0)

                    return if(intersection > 0 && intersection < minIt) {
                        Triple(vec3(0, 0, -1), 0.5, intersection)
                    } else {
                        null
                    }
                }

                fun drawBox() : Triple<Vec3, Double, Double>? {
                    val output = box(ro, rd, vec3(1, 1, 1), vec3(0, 0, 0))

                    val intersection = output.first
                    val n = output.second

                    return if(intersection.x > 0 && intersection.x < minIt) {
                        Triple(n, 1.0, intersection.x)
                    } else {
                        null
                    }
                }

                fun render() : Pair<Vec3, Double> {
                    var n = vec3(0, 0, 0)
                    var albedo = 1.0

                    fun processTriple(
                        triple : Triple<Vec3, Double, Double>?
                    ) {
                        if(triple != null) {
                            n = triple.first
                            albedo = triple.second
                            minIt = triple.third
                        }
                    }

                    drawSphere(vec3(0, 2, 0)).also { processTriple(it) }
                    drawBox().also { processTriple(it) }
//                    drawPlane().also { processTriple(it) }

                    return Pair(n, albedo)
                }

                fun handleRender() {
                    run {
                        for(k in 0 until 5) {
                            var n : Vec3
                            var albedo : Double

                            render().also {
                                n = it.first
                                albedo = it.second
                            }

                            if(minIt < 99999.0) {
                                diff *= (dot(n, light) * 0.5 + 0.5) * albedo * 2
                                ro += rd * (minIt - 0.01)
                                rd = reflect(rd, n)
                                needColor = true
                            } else {
                                return@run
                            }
                        }
                    }
                }

                handleRender()

                val color = if(needColor) {
                    clamp((diff * 20).toInt(), 0, gradientSize)
                } else {
                    0
                }

                val pixel = gradient[color]

                matrix[vec2(i, j)] = pixel.toString()
            }
        }

        val lines = mutableMapOf<Int, String>()

        for(entry in matrix.entries) {
            if(lines.containsKey(entry.key.y.toInt())) {
                lines[entry.key.y.toInt()] += entry.value
            } else {
                lines[entry.key.y.toInt()] = entry.value
            }
        }

        var frame = ""

        for(line in lines.values) {
            frame += "$line\n"
        }

        print(frame)
    }
}

fun clamp(
    value : Int,
    min : Int,
    max : Int
) : Int = max(min(value, max), min)

fun reflect(
    rd : Vec3,
    n : Vec3
) : Vec3 = rd - n * (2 * dot(n, rd))

fun abs(
    vec : Vec3
) : Vec3 = vec3(abs(vec.x), abs(vec.y), abs(vec.z))

fun sign(
    n : Double
) : Double = if(n == 0.0) {
    0.0
} else {
    if(n < 0.0) {
        -1.0
    } else {
        1.0
    }
}

fun sign(
    vec : Vec3
) : Vec3 = vec3(sign(vec.x), sign(vec.y), sign(vec.z))

fun step(
    edge : Double,
    n : Double
) : Double = if(n > edge) {
    1.0
} else {
    0.0
}

fun step(
    edge : Vec3,
    vec : Vec3
) : Vec3 = vec3(step(edge.x, vec.x), step(edge.y, vec.y), step(edge.z, vec.z))

fun box(
    ro : Vec3,
    rd : Vec3,
    size : Vec3,
    normal0 : Vec3
) : Pair<Vec2, Vec3> {
    val m = vec3(1, 1, 1) / rd
    val n = m * ro
    val k = abs(m) * size
    val t1 = -n - k
    val t2 = -n + k
    val tN = max((max(t1.x, t1.y)), t1.z)
    val tF = min((min(t2.x, t2.y)), t2.z)

    return if(tN > tF || tF < 0) {
        Pair(vec2(-1, -1), normal0)
    } else {
        val yzx = vec3(t1.y, t1.z, t1.x)
        val zxy = vec3(t1.z, t1.x, t1.y)
        val normal1 = -sign(rd) * step(yzx, t1) * step(zxy, t1)

        Pair(vec2(tN, tF), normal1)
    }
}

fun plane(
    ro : Vec3,
    rd : Vec3,
    p : Vec3,
    w : Double
) : Double = -(dot(ro, p) + w) / dot(rd, p)

fun sphere(
    ro : Vec3,
    rd : Vec3,
    radius : Double
) : Vec2 {
    val b = dot(ro, rd)
    val c = dot(ro, ro) - radius * radius
    var h = b * b - c

    return if(h < 0) {
        vec2(-1, -1)
    } else {
        h = sqrt(h)

        vec2(-b - h, -b + h)
    }
}

fun dot(
    a : Vec3,
    b : Vec3
) : Double = a.x * b.x + a.y * b.y + a.z * b.z

fun rotateX(
    vec : Vec3,
    angle : Double
) {
    vec.z = vec.z * cos(angle) - vec.y * sin(angle)
    vec.y = vec.z * sin(angle) + vec.y * cos(angle)
}

fun rotateY(
    vec : Vec3,
    angle : Double
) {
    vec.x = vec.x * cos(angle) - vec.z * sin(angle)
    vec.z = vec.x * sin(angle) + vec.z * cos(angle)
}

fun rotateZ(
    vec : Vec3,
    angle : Double
) {
    vec.x = vec.x * cos(angle) - vec.y * sin(angle)
    vec.y = vec.x * sin(angle) + vec.y * cos(angle)
}

fun vec2(
    x : Number,
    y : Number
) : Vec2 = Vec2(x.toDouble(), y.toDouble())

fun vec3(
    x : Number,
    y : Number,
    z : Number
) : Vec3 = Vec3(x.toDouble(), y.toDouble(), z.toDouble())

fun length(
    vec : Vec2
) : Double = sqrt(vec.x * vec.x + vec.y * vec.y)

fun length(
    vec : Vec3
) : Double = sqrt(vec.x * vec.x + vec.y * vec.y + vec.z * vec.z)

fun norm(
    vec : Vec3
) : Vec3 = vec / length(vec)

class Vec2(
    var x : Double,
    var y : Double
) {
    operator fun div(
        vec : Vec2
    ) : Vec2 = Vec2(x / vec.x, y / vec.y)

    operator fun div(
        n : Double
    ) : Vec2 = Vec2(x / n, y / n)

    operator fun times(
        vec : Vec2
    ) : Vec2 = Vec2(x * vec.x, y * vec.y)

    operator fun times(
        n : Number
    ) : Vec2 = Vec2(x * n.toDouble(), y * n.toDouble())

    operator fun minus(
        vec : Vec2
    ) : Vec2 = Vec2(x - vec.x, y - vec.y)

    operator fun minus(
        n : Number
    ) : Vec2 = Vec2(x - n.toDouble(), y - n.toDouble())
}

class Vec3(
    var x : Double,
    var y : Double,
    var z : Double
) {
    operator fun div(
        vec : Vec3
    ) : Vec3 = Vec3(x / vec.x, y / vec.y, z / vec.z)

    operator fun div(
        n : Double
    ) : Vec3 = Vec3(x / n, y / n, z / n)

    operator fun times(
        vec : Vec3
    ) : Vec3 = Vec3(x * vec.x, y * vec.y, z * vec.z)

    operator fun times(
        n : Number
    ) : Vec3 = Vec3(x * n.toDouble(), y * n.toDouble(), z * n.toDouble())

    operator fun minus(
        vec : Vec3
    ) : Vec3 = Vec3(x - vec.x, y - vec.y, z - vec.z)

    operator fun minus(
        n : Number
    ) : Vec3 = Vec3(x - n.toDouble(), y - n.toDouble(), z - n.toDouble())

    operator fun plus(
        vec : Vec3
    ) : Vec3 = Vec3(x + vec.x, y + vec.y, z + vec.z)

    operator fun plus(
        n : Number
    ) : Vec3 = Vec3(x + n.toDouble(), y + n.toDouble(), z + n.toDouble())

    operator fun unaryMinus() : Vec3 = Vec3(-x, -y, -z)
}